# Board setup log

State of the Radxa Zero 3W as of 2026-10-29.

## OS
Armbian minimal, vendor kernel (see `image/IMAGE.md`). Flashed with `scripts/flash-sd.sh`.

- Hostname `action-cam`, user `radxa` (sudo), SSH key `~/.ssh/action_cam` on the laptop.
- Wi-Fi: netplan + systemd-networkd, `/etc/netplan/20-wifi.yaml` with two access points
  (home + phone hotspot). Signal at home: -43 dBm with the external antenna.
- No avahi yet, so find the board by IP (home DHCP gave it 192.168.1.131).
- Idle temperature 46–50 °C with no heatsink.

### Armbian first login did not run headless
The preset in `/root/.not_logged_in_yet` only runs on a console login. With no screen nothing logs in,
and running `armbian-firstlogin` over SSH hung. Fix used: log in as root with the key,
create the user and set passwords by hand, delete `/root/.not_logged_in_yet`, remove the getty
autologin overrides. Watch out: `pkill -f armbian-firstlogin` also kills your own SSH command.

## Radxa OS desktop image ran very hot
The first image was Radxa OS with GDM/Xorg. It ran hot even with nothing attached. Replaced by
Armbian minimal.

## Camera (CSI, 22-pin)
- Overlay `radxa-zero3-rpi-camera-v1.3` (OV5647) is enabled in `/boot/armbianEnv.txt`
  (copy in `rootfs/boot/armbianEnv.txt`). Armbian's boot script also accepts overlay names
  without the `rk35xx-` prefix.
- Cable: contacts face the side of the slot that has the contact springs. Usually the blue
  stiffener faces away from the board. Some Zero 3W batches have the clip mounted upside down.
- **First attempt was inserted the wrong way: the board got very hot and did not boot.**
- Second attempt: board boots and stays cool, ISP pipeline comes up (`/dev/video0` = `rkisp_mainpath`),
  but the sensor does not answer on I2C (`ov5647 2-0036: i2c write error, reg: 3000`),
  so the driver never binds and capture fails. Reseating the sensor's snap connector did not help.
  The module is most likely damaged. A replacement camera is on the way.

Useful checks:
```
readlink /sys/bus/i2c/devices/2-0036/driver     # empty = sensor not detected
sudo dmesg | grep -i ov5647
echo 2-0036 | sudo tee /sys/bus/i2c/drivers/ov5647/bind   # retry detection
v4l2-ctl --list-devices; media-ctl -p
```

Supported alternatives on this board: Raspberry Pi Camera v2 (IMX219) with
`radxa-zero3-rpi-camera-v2`, Radxa Camera 8M.

Packages added: `v4l-utils`, `i2c-tools`, `device-tree-compiler`, `gpiod`.

## Plan B: USB webcam (while waiting for a new CSI camera)
A disassembled USB webcam board has four pads: **V** = +5 V, **G** = GND, **P** = D+, **M** = D−.
It is still a USB device, so it cannot go on GPIO pins. Wiring:
- Solder a cut USB-A cable to the pads (red→V, black→G, green→P, white→M). Keep the data wires
  short (<15 cm) and twisted.
- USB-A female → USB-C **OTG** adapter → the Zero 3W's USB 3.0 Type-C port (not the power one).
  An OTG adapter makes the port supply 5 V; a plain USB-C cable end may not.
- Not detected? Swap P and M first. Check V/G twice before plugging in.

Next steps when it is plugged in: `lsusb`, `v4l2-ctl --list-devices --list-formats-ext`,
run `scripts/deploy-server.sh`, add a `webcam` source to `server/camd.py`
(photo = one frame, recording = MJPEG stream copy via `ffmpeg -f v4l2 -input_format mjpeg`, little CPU),
set `"source": "webcam"` in `/etc/action-cam/camd.json`.
