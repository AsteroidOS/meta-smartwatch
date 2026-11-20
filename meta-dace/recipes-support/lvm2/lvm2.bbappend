# dace: no usa device-mapper (el rootfs va directo en userdata) y dmeventd
# falla al linkar con gcc 14 en el host de build.
LVM2_PACKAGECONFIG:dace = ""
PACKAGECONFIG:remove:dace = "dmeventd"
