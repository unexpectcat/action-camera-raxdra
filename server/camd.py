#!/usr/bin/env python3
"""Action camera daemon: HTTP API for the phone app + UDP discovery.

Standard library only. Capture and recording run external commands from the config
(ffmpeg test pattern by default, GStreamer for the real camera), so changing the camera
only means changing camd.json.

API (all JSON unless noted):
  GET  /api/info                 name, recording state
  POST /api/photo                take one photo, returns image/jpeg
  GET  /api/photo                last photo, image/jpeg (404 if none)
  POST /api/record/start         body {"name": "2026-10-29_12-30-05"} (phone's local time)
  POST /api/record/stop
  GET  /api/videos               [{name, size, time, recording}]
  GET  /api/videos/<file>        the file, supports Range
Discovery: send "ACTIONCAM?" to UDP 41234, the reply is the /api/info JSON plus "port".
"""
import json
import os
import random
import re
import shlex
import signal
import socket
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DEFAULTS = {
    "name": socket.gethostname(),
    "port": 8080,
    "discovery_port": 41234,
    "media_dir": os.path.expanduser("~/videos"),
    "source": "test",
    "sources": {
        # generated picture, no camera needed; low resolution keeps the CPU (and heat) down
        "test": {
            "photo": "ffmpeg -loglevel error -y -f lavfi -i testsrc2=size=1280x720:rate=1 "
                     "-vf hue=h={hue} -frames:v 1 -q:v 3 {out}",
            "record": "ffmpeg -loglevel error -y -re -f lavfi -i testsrc2=size=640x480:rate=25 "
                      "-c:v libx264 -preset ultrafast -pix_fmt yuv420p "
                      "-movflags +frag_keyframe+empty_moov+default_base_moof {out}",
            "photo_while_recording": True,
        },
        # Rockchip ISP + hardware H.264 encoder (needs a working CSI camera)
        "camera": {
            "photo": "gst-launch-1.0 -q v4l2src device=/dev/video0 num-buffers=15 ! "
                     "video/x-raw,format=NV12,width=1920,height=1080 ! videoconvert ! jpegenc ! "
                     "multifilesink location={out}",
            "record": "gst-launch-1.0 -q -e v4l2src device=/dev/video0 ! "
                      "video/x-raw,format=NV12,width=1920,height=1080,framerate=30/1 ! "
                      "mpph264enc ! h264parse ! mp4mux fragment-duration=1000 ! filesink location={out}",
            "photo_while_recording": False,
        },
    },
}

NAME_RE = re.compile(r"^[0-9A-Za-z_\-. ]{1,64}$")


def load_config():
    cfg = json.loads(json.dumps(DEFAULTS))
    path = os.environ.get("CAMD_CONFIG", "/etc/action-cam/camd.json")
    if os.path.exists(path):
        with open(path) as f:
            user = json.load(f)
        sources = user.pop("sources", {})
        cfg.update(user)
        for k, v in sources.items():
            cfg["sources"].setdefault(k, {}).update(v)
    for k in ("name", "media_dir", "source"):
        env = os.environ.get("CAMD_" + k.upper())
        if env:
            cfg[k] = env
    if os.environ.get("CAMD_PORT"):
        cfg["port"] = int(os.environ["CAMD_PORT"])
    return cfg


class Camera:
    def __init__(self, cfg):
        self.cfg = cfg
        self.src = cfg["sources"][cfg["source"]]
        self.dir = cfg["media_dir"]
        os.makedirs(self.dir, exist_ok=True)
        self.photo_path = os.path.join(self.dir, ".last-photo.jpg")
        self.lock = threading.Lock()
        self.proc = None
        self.rec_name = None
        self.rec_started = 0.0

    def recording(self):
        if self.proc and self.proc.poll() is not None:
            print(f"recorder exited with {self.proc.returncode}", file=sys.stderr)
            self.proc, self.rec_name = None, None
        return self.proc is not None

    def info(self):
        rec = self.recording()
        return {
            "name": self.cfg["name"],
            "source": self.cfg["source"],
            "recording": rec,
            "recording_name": self.rec_name if rec else None,
            "recording_seconds": int(time.time() - self.rec_started) if rec else 0,
            "has_photo": os.path.exists(self.photo_path),
        }

    def photo(self):
        with self.lock:
            if self.recording() and not self.src.get("photo_while_recording"):
                raise Busy("camera is recording")
            tmp = self.photo_path + ".tmp.jpg"
            cmd = self.src["photo"].format(out=shlex.quote(tmp), hue=random.randint(0, 359))
            subprocess.run(cmd, shell=True, check=True, timeout=30)
            os.replace(tmp, self.photo_path)
        with open(self.photo_path, "rb") as f:
            return f.read()

    def start(self, name):
        with self.lock:
            if self.recording():
                return
            if not NAME_RE.match(name or ""):
                name = time.strftime("%Y-%m-%d_%H-%M-%S")
            out = os.path.join(self.dir, name + ".mp4")
            n = 1
            while os.path.exists(out):
                n += 1
                out = os.path.join(self.dir, f"{name}_{n}.mp4")
            cmd = self.src["record"].format(out=shlex.quote(out))
            # own process group so stop() reaches every process of the pipeline
            self.proc = subprocess.Popen(cmd, shell=True, start_new_session=True, stdin=subprocess.DEVNULL)
            self.rec_name = os.path.basename(out)
            self.rec_started = time.time()

    def stop(self):
        with self.lock:
            if not self.proc:
                return
            # SIGINT lets ffmpeg/gst (-e) finish the file properly
            try:
                os.killpg(self.proc.pid, signal.SIGINT)
                self.proc.wait(timeout=10)
            except (subprocess.TimeoutExpired, ProcessLookupError):
                try:
                    os.killpg(self.proc.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
            self.proc, self.rec_name = None, None

    def videos(self):
        rec = self.rec_name if self.recording() else None
        out = []
        for e in os.scandir(self.dir):
            if e.is_file() and e.name.endswith(".mp4"):
                st = e.stat()
                out.append({"name": e.name, "size": st.st_size, "time": int(st.st_mtime),
                            "recording": e.name == rec})
        return sorted(out, key=lambda v: v["name"], reverse=True)


class Busy(Exception):
    pass


def make_handler(cam):
    class Handler(BaseHTTPRequestHandler):
        protocol_version = "HTTP/1.1"

        def log_message(self, fmt, *args):
            pass

        def send(self, code, body=b"", ctype="application/json", headers=None):
            if isinstance(body, (dict, list)):
                body = json.dumps(body).encode()
            self.send_response(code)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            for k, v in (headers or {}).items():
                self.send_header(k, v)
            self.end_headers()
            if self.command != "HEAD":
                self.wfile.write(body)

        def body_json(self):
            n = int(self.headers.get("Content-Length") or 0)
            if not n:
                return {}
            try:
                return json.loads(self.rfile.read(n))
            except ValueError:
                return {}

        def do_GET(self):
            p = self.path.split("?")[0]
            if p == "/api/info":
                return self.send(200, cam.info())
            if p == "/api/photo":
                if not os.path.exists(cam.photo_path):
                    return self.send(404, {"error": "no photo yet"})
                with open(cam.photo_path, "rb") as f:
                    return self.send(200, f.read(), "image/jpeg")
            if p == "/api/videos":
                return self.send(200, cam.videos())
            if p.startswith("/api/videos/"):
                return self.send_video(p[len("/api/videos/"):])
            self.send(404, {"error": "not found"})

        do_HEAD = do_GET

        def do_POST(self):
            p = self.path.split("?")[0]
            try:
                if p == "/api/photo":
                    return self.send(200, cam.photo(), "image/jpeg")
                if p == "/api/record/start":
                    cam.start(self.body_json().get("name"))
                    return self.send(200, cam.info())
                if p == "/api/record/stop":
                    self.body_json()
                    cam.stop()
                    return self.send(200, cam.info())
            except Busy as e:
                return self.send(409, {"error": str(e)})
            except Exception as e:  # report to the app instead of dropping the connection
                return self.send(500, {"error": str(e)})
            self.send(404, {"error": "not found"})

        def send_video(self, name):
            from urllib.parse import unquote
            name = os.path.basename(unquote(name))
            path = os.path.join(cam.dir, name)
            if not name.endswith(".mp4") or not os.path.isfile(path):
                return self.send(404, {"error": "no such video"})
            size = os.path.getsize(path)
            start, end = 0, size - 1
            m = re.match(r"bytes=(\d*)-(\d*)", self.headers.get("Range", ""))
            if m and (m.group(1) or m.group(2)):
                if m.group(1):
                    start = int(m.group(1))
                    end = int(m.group(2)) if m.group(2) else end
                else:
                    start = max(0, size - int(m.group(2)))
            length = end - start + 1
            self.send_response(206 if m else 200)
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Content-Length", str(length))
            if m:
                self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
            self.end_headers()
            if self.command == "HEAD":
                return
            with open(path, "rb") as f:
                f.seek(start)
                left = length
                while left > 0:
                    chunk = f.read(min(256 * 1024, left))
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    left -= len(chunk)

    return Handler


def discovery(cam, cfg):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(("0.0.0.0", cfg["discovery_port"]))
    while True:
        data, addr = s.recvfrom(256)
        if data.strip() == b"ACTIONCAM?":
            reply = dict(cam.info(), port=cfg["port"])
            s.sendto(json.dumps(reply).encode(), addr)


def main():
    cfg = load_config()
    cam = Camera(cfg)
    threading.Thread(target=discovery, args=(cam, cfg), daemon=True).start()
    server = ThreadingHTTPServer(("0.0.0.0", cfg["port"]), make_handler(cam))
    server.daemon_threads = True

    def shutdown(*_):
        cam.stop()
        threading.Thread(target=server.shutdown).start()

    signal.signal(signal.SIGTERM, shutdown)
    print(f"camd '{cfg['name']}' on :{cfg['port']}, source={cfg['source']}, media={cam.dir}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        cam.stop()


if __name__ == "__main__":
    main()
