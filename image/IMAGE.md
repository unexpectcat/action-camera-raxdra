# Base image

Armbian 26.8.1, Debian 13 (trixie), **vendor** kernel 6.1.115, **minimal** (no desktop).

- URL: https://dl.armbian.com/radxa-zero3/archive/Armbian_26.8.1_Radxa-zero3_trixie_vendor_6.1.115_minimal.img.xz
- SHA256: `e78de9cb4b2d8ff1710c65c509d08faef1ba88c07fc3276b9dd637597e8234e8`

Vendor (Rockchip BSP) kernel instead of mainline: it supports the RK3566 hardware
video encoder (MPP) and camera ISP, which is what makes the board usable as a camera.
