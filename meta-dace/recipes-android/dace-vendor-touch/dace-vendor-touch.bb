SUMMARY = "Stock vendor modules (Mobvoi) for the Zinitix touch and the slate event stack"
DESCRIPTION = "The prebuilt .ko's from the stock OTA, already PATCHED for our \
kernel (WITHOUT __versions: forced load; SCS NOPed and exit reloc repositioned, \
see files/README.txt and patch-stock-module.py). Without the SCS patch, the \
rootfs boot crashes (x18 is garbage in our kernel: it has no \
SHADOW_CALL_STACK). They require the cfi_check ABI slot in struct module \
(dace-module-cfi-abi-slot.patch). They are installed OUTSIDE /lib/modules so \
udev/depmod do not autoload them: dace-lxc-hal-start.sh loads them after the \
display HALs, in order."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"

SRC_URI = "file://slate_events_bridge.ko \
           file://slate_events_bridge_rpmsg.ko \
           file://slate_mobvoi_rpc.ko \
           file://slate_mobvoi_rpc_rpmsg.ko \
           file://zinitix-i2c.ko"

S = "${UNPACKDIR}"

# Destination deliberately OUTSIDE /lib/modules: no depmod, no udev autoload
# (the vendor module coldplug left the SoC in reset).
# Exact load order (dace-lxc-hal-start.sh):
#   slate_events_bridge_rpmsg -> slate_events_bridge ->
#   slate_mobvoi_rpc_rpmsg -> slate_mobvoi_rpc -> zinitix-i2c
do_install() {
    install -d ${D}${nonarch_base_libdir}/dace-vendor-modules
    for f in slate_events_bridge.ko slate_events_bridge_rpmsg.ko \
             slate_mobvoi_rpc.ko slate_mobvoi_rpc_rpmsg.ko zinitix-i2c.ko; do
        install -m 0644 ${UNPACKDIR}/${f} \
            ${D}${nonarch_base_libdir}/dace-vendor-modules/${f}
    done
}

FILES:${PN} = "${nonarch_base_libdir}/dace-vendor-modules/*"
# No strip or debug split: they are stock prebuilts, do not touch the binaries.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
# The .ko's are KERNEL ones (aarch64); the package userland is ARM32: the arch QA
# would always fail. They are kernel modules, not userspace binaries.
INSANE_SKIP:${PN} = "arch"
# Nothing to stage into the sysroot: avoids the sysroot strip (crosstool) trying
# to process the aarch64 .ko's ("file format not recognized").
SYSROOT_DIRS = ""
PACKAGE_ARCH = "${MACHINE_ARCH}"
