# dace: no usa device-mapper (el rootfs va directo en userdata). dmeventd
# falla al linkar (undefined dm_* con gcc 14); se apaga solo en dace.
LVM2_PACKAGECONFIG:dace = ""
PACKAGECONFIG:remove:dace = "dmeventd"
