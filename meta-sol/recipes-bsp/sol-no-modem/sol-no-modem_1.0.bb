# sol has no modem: unbind the modem remoteproc so nothing can start it.
# On a unit reporting modem_supported=0x0, 6080000.remoteproc-mss fails its
# firmware load and subsystem-restart escalates to a full SoC reset, about ten
# seconds after the Android container starts. See the unit for details.
#
# sol ships in LTE and BT/WiFi-only variants. This is for the latter only; a
# modem-equipped unit must not get it.
SUMMARY = "Unbind the modem remoteproc on modem-less sol units"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-unbind-modem-remoteproc.service"

S = "${UNPACKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "sol-unbind-modem-remoteproc.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/sol-unbind-modem-remoteproc.service \
        ${D}${systemd_system_unitdir}/sol-unbind-modem-remoteproc.service
}

FILES:${PN} = "${systemd_system_unitdir}/sol-unbind-modem-remoteproc.service"
