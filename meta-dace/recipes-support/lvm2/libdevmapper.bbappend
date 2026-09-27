# El T5 no usa device-mapper (el rootfs va en userdata directo). dmeventd
# falla al linkear (undefined dm_* con gcc 14); lo apagamos del build.
LVM2_PACKAGECONFIG = ""
PACKAGECONFIG:remove = "dmeventd"
