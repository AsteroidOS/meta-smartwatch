FILESEXTRAPATHS:prepend:emulator := "${THISDIR}/linux-yocto:"
SRC_URI:append:emulator = " file://drm-virtio-gpu.cfg "

KERNEL_CONFIG_FRAGMENTS:append:emulator = " ${WORKDIR}/drm-virtio-gpu.cfg "
COMPATIBLE_MACHINE = "emulator"
