SUMMARY = "Auto-load post-rootfs aurora kernel modules at boot"
DESCRIPTION = "Drops a systemd-modules-load.d config that loads Aurora's \
post-rootfs kernel modules. These modules cannot live in modules.load.aurora \
because their .ko files are only reachable AFTER pivot_root to the rootfs \
(the VKB ramdisk holds only the boot-critical modules)."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "aurora"

SRC_URI = "file://aurora-post-rootfs.conf \
           file://aurora-wlan-wowlan.service"
S = "${UNPACKDIR}"

inherit systemd

do_install() {
    install -d -m 0755 ${D}${sysconfdir}/modules-load.d
    install -m 0644 ${UNPACKDIR}/aurora-post-rootfs.conf ${D}${sysconfdir}/modules-load.d/aurora-post-rootfs.conf

    install -d -m 0755 ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/aurora-wlan-wowlan.service ${D}${systemd_system_unitdir}/aurora-wlan-wowlan.service
}

FILES:${PN} = "${sysconfdir}/modules-load.d/aurora-post-rootfs.conf \
               ${systemd_system_unitdir}/aurora-wlan-wowlan.service"

RDEPENDS:${PN} = "iw linux-aurora-modules"

SYSTEMD_SERVICE:${PN} = "aurora-wlan-wowlan.service"
