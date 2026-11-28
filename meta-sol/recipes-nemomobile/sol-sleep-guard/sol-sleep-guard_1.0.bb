SUMMARY = "Recover the watch when mce stops arming autosleep"
DESCRIPTION = "mce can hold its mce_display_on wakelock while the display is \
off, which stops the kernel arming autosleep at all -- the watch then never \
attempts a suspend and quietly drains. This notices that specific state and \
nudges mce back, leaving a watch that is merely failing suspends (a wakeup \
source problem) alone."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"
COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-sleep-guard.sh \
           file://sol-sleep-guard.service \
           file://sol-sleep-guard.timer"

inherit systemd

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "mce"

SYSTEMD_SERVICE:${PN} = "sol-sleep-guard.timer"
SYSTEMD_AUTO_ENABLE = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/sol-sleep-guard.sh ${D}${bindir}/

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/sol-sleep-guard.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${UNPACKDIR}/sol-sleep-guard.timer ${D}${systemd_system_unitdir}/
}

FILES:${PN} = "${bindir}/sol-sleep-guard.sh \
               ${systemd_system_unitdir}/sol-sleep-guard.service \
               ${systemd_system_unitdir}/sol-sleep-guard.timer"
