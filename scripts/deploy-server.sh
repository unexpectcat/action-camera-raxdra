#!/bin/bash
# Install or update camd on the board: ./scripts/deploy-server.sh [ip]
# Uses the SSH key and sudo password from secrets.env. The board has no SFTP, so files go over ssh.
set -euo pipefail
cd "$(dirname "$0")/.."
. ./secrets.env
HOST="${1:-${BOARD_IP:-192.168.1.131}}"
SSH=(ssh -i "${SSH_KEY/#\~/$HOME}" -o IdentitiesOnly=yes "$USER_NAME@$HOST")
sudo_run() { "${SSH[@]}" "echo '$USER_PASSWORD' | sudo -S -p '' sh -c '$1'"; }

echo "== packages"
sudo_run "DEBIAN_FRONTEND=noninteractive apt-get install -y -qq ffmpeg gstreamer1.0-tools gstreamer1.0-plugins-good v4l-utils >/dev/null"

echo "== files"
"${SSH[@]}" "cat > /tmp/camd.py" < server/camd.py
"${SSH[@]}" "cat > /tmp/action-cam.service" < server/action-cam.service
sudo_run "install -D -m 755 /tmp/camd.py /opt/action-cam/camd.py && install -m 644 /tmp/action-cam.service /etc/systemd/system/action-cam.service"
# keep an existing config (it says which camera source to use); default to the test picture
sudo_run "test -f /etc/action-cam/camd.json || { mkdir -p /etc/action-cam; echo '{\"name\": \"action-cam\", \"source\": \"test\"}' > /etc/action-cam/camd.json; }"

echo "== service"
sudo_run "systemctl daemon-reload && systemctl enable action-cam && systemctl restart action-cam"
sleep 2
curl -s --max-time 5 "http://$HOST:8080/api/info" && echo
