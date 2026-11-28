SUMMARY = "sol: follow real cable state so USB can enter LPM and the SoC can suspend"
DESCRIPTION = "sol's extcon-usb-shim ships spoof_connected=1, which fakes a \
permanently attached cable and makes every suspend abort with 'USB is outside \
LPM'. Clearing it permanently is not an option either: sol's gadget never \
re-enumerates after a cable loss, and the only recovery -- sol-usb-recover's \
dwc3 unbind -- is proven safe only while the spoof is 1. This tracks the \
charger instead, so USB sleeps off charger and recovery stays available on it."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://sol-usb-lpm.sh \
           file://sol-usb-lpm.service"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/sol-usb-lpm.sh ${D}${bindir}/sol-usb-lpm.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-usb-lpm.service \
        ${D}${systemd_unitdir}/system/sol-usb-lpm.service
}

SYSTEMD_SERVICE:${PN} = "sol-usb-lpm.service"
# Enabled: suspend AND resume are both verified on hardware now -- 101 deep
# suspend/resume cycles, uptime continuous, everything healthy afterwards. The
# watch also wakes on the cable returning, which is what made this safe to turn
# on. Leaving it off is what silently cost a whole test round, since after a
# reboot nothing cleared the spoof and the watch simply never slept.
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "${bindir}/sol-usb-lpm.sh \
               ${systemd_unitdir}/system/sol-usb-lpm.service"
