# action-camera-raxdra

Budget action camera built on a **Radxa Zero 3W** (RK3566) with an SD card and a Wi-Fi antenna.

## Layout
- `image/` – which OS image is used and why.
- `rootfs/` – files placed on the board, mirroring its filesystem. `*.example*` files are templates; real values live in `secrets.env`.
- `scripts/flash-sd.sh` – flashes the image to the SD card and injects first-boot config (run as root).
- `docs/` – notes.

## Setup
1. `cp secrets.env.example secrets.env` and fill it in. Generate a key: `ssh-keygen -t ed25519 -f ~/.ssh/action_cam`.
2. Download and verify the image from `image/IMAGE.md`, decompress it.
3. Render the templates into a staging dir (`not_logged_in_yet`, `20-wifi.yaml`, `hostname`, `authorized_keys`).
4. `sudo scripts/flash-sd.sh <image.img> <staging-dir>`
5. Boot the board. First boot creates the user, sets the timezone and joins whichever saved Wi-Fi is in range.
6. `ssh -i ~/.ssh/action_cam radxa@<ip>`

## Private data
Wi-Fi names and passwords, account passwords and SSH keys are gitignored and must never be committed.
