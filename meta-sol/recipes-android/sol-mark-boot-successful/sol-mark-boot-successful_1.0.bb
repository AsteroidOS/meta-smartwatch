SUMMARY = "Mark the A/B boot slot successful so the bootloader keeps booting it"
DESCRIPTION = "sol's bootloader decrements the slot's tries-remaining on every \
boot and expects userspace to mark the slot successful. Nothing did, so the \
counter reached zero and the bootloader declared the slot unbootable and \
dropped to fastboot, needing a host to recover. AOSP marks the slot from \
update_verifier on sys.boot_completed, a property AsteroidOS never sets, so the \
binary shipped in the container image but never ran."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"
COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-mark-boot-successful.sh \
           file://sol-mark-boot-successful.service"

inherit systemd

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "lxc"

SYSTEMD_SERVICE:${PN} = "sol-mark-boot-successful.service"
SYSTEMD_AUTO_ENABLE = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/sol-mark-boot-successful.sh ${D}${bindir}/
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/sol-mark-boot-successful.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} = "${bindir}/sol-mark-boot-successful.sh \
               ${systemd_system_unitdir}/sol-mark-boot-successful.service"
