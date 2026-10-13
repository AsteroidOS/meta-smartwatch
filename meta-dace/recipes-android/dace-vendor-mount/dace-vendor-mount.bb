SUMMARY = "Recreate the dm-linear mappings for the T5's /super partitions and mount them"
DESCRIPTION = "The T5 (TicWatch Pro 5, monaco/SW5100) ships Wear OS 13 with \
dynamic partitions: system/vendor/product/system_ext/vendor_dlkm/system_dlkm \
live inside /super (/dev/mmcblk0p7, 4 GiB) and Android exposes them via \
dm-linear from its first-stage init. Our initramfs is a shell script that does \
not parse the LP metadata, so the service recreates the tables (read with \
lpdump from the real T5 super metadata, geometry at offset 4096) and mounts the \
devices at /android/* -- which is what the LXC container bind-mounts into its \
rootfs -- plus the symlinks /vendor -> /android/vendor and \
/system -> /var/lib/lxc/android/rootfs/system that Halium and libhybris expect. \
Without this the launcher aborts with 'failed to find/load gralloc'."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "dace"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://dace-vendor-mount.sh \
           file://dace-vendor-mount.service"
S = "${UNPACKDIR}"

RDEPENDS:${PN} = "lvm2 util-linux-mount"

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/dace-vendor-mount.sh ${D}${libexecdir}/dace-vendor-mount.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/dace-vendor-mount.service \
        ${D}${systemd_unitdir}/system/dace-vendor-mount.service

    install -d ${D}${sysconfdir}/systemd/system/local-fs.target.wants
    ln -sf ../../../systemd/system/dace-vendor-mount.service \
        ${D}${sysconfdir}/systemd/system/local-fs.target.wants/dace-vendor-mount.service

}

SYSTEMD_SERVICE:${PN} = "dace-vendor-mount.service"
FILES:${PN} = "${libexecdir}/dace-vendor-mount.sh \
               ${systemd_unitdir}/system/dace-vendor-mount.service \
               ${sysconfdir}/systemd/system/local-fs.target.wants/dace-vendor-mount.service"
