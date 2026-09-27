# Igual para lvm2: sin dmeventd (no se usa; rompe el link con gcc 14).
LVM2_PACKAGECONFIG = ""
PACKAGECONFIG:remove = "dmeventd"
