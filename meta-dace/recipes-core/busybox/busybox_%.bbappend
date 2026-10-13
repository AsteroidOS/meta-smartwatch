FILESEXTRAPATHS:prepend:dace := "${THISDIR}/busybox:"

# dace-only: enable busybox "sync -f" (syncfs(2)). The initramfs needs it to
# checkpoint the F2FS userdata partition after asteroidos.ext4 is pushed over
# adb; the default oe-core config leaves FEATURE_SYNC_FANCY off, so plain
# "sync" does not checkpoint and a hard reboot can lose the image.
SRC_URI:append:dace = " file://syncfs.cfg"
