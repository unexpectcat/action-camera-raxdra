#!/bin/bash
# Flash Armbian to the SD card and inject first-boot config. Run as root.
set -euo pipefail
IMG="$1" STAGE="$2" DEV=/dev/sda

# Safety: only write to a removable ~29G disk
[[ "$(lsblk -dno RM $DEV)" == 1 ]] || { echo "$DEV is not removable, abort"; exit 1; }
SIZE=$(lsblk -bdno SIZE $DEV); (( SIZE > 20000000000 && SIZE < 40000000000 )) || { echo "unexpected size $SIZE, abort"; exit 1; }

for p in $(lsblk -lno NAME $DEV | tail -n +2); do umount "/dev/$p" 2>/dev/null || true; done
wipefs -a $DEV >/dev/null
dd if="$IMG" of=$DEV bs=4M conv=fsync status=progress
partprobe $DEV; udevadm settle; sleep 2

M=$(mktemp -d /run/action-cam.XXXX)
mount ${DEV}1 "$M"
install -m 600 "$STAGE/not_logged_in_yet" "$M/root/.not_logged_in_yet"
install -m 600 "$STAGE/20-wifi.yaml" "$M/etc/netplan/20-wifi.yaml"
H=$(cat "$STAGE/hostname")
echo "$H" > "$M/etc/hostname"
sed -i "s/radxa-zero3/$H/g" "$M/etc/hosts"
install -d -m 700 "$M/etc/skel/.ssh" "$M/root/.ssh"
install -m 600 "$STAGE/authorized_keys" "$M/etc/skel/.ssh/authorized_keys"
install -m 600 "$STAGE/authorized_keys" "$M/root/.ssh/authorized_keys"
sync; umount "$M"; rmdir "$M"
echo "DONE"
