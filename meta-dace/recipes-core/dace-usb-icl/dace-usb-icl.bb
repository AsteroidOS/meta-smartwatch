SUMMARY = "dace: set the USB SDP current so smblite charges"
DESCRIPTION = "smblite starts with usb_icl_votable at ~2 mA, so on an SDP port \
(PC) the input is left suspended and the watch does not charge. The Android USB \
driver sets that limit via POWER_SUPPLY_PROP_INPUT_CURRENT_LIMIT; there is no \
Android framework here, so a service + a udev rule write 500 mA to \
usb/input_current_limit when USB is present and online=0."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "dace"

inherit systemd

SRC_URI = "file://dace-usb-icl.sh \
           file://dace-usb-icl.service \
           file://99-dace-usb-icl.rules"

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "dace-usb-icl.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d -m 0755 ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/dace-usb-icl.sh ${D}${libexecdir}/dace-usb-icl.sh

    install -d -m 0755 ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/dace-usb-icl.service \
        ${D}${systemd_system_unitdir}/dace-usb-icl.service

    install -d -m 0755 ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/99-dace-usb-icl.rules \
        ${D}${sysconfdir}/udev/rules.d/99-dace-usb-icl.rules
}

FILES:${PN} = "${libexecdir}/dace-usb-icl.sh \
               ${systemd_system_unitdir}/dace-usb-icl.service \
               ${sysconfdir}/udev/rules.d/99-dace-usb-icl.rules"