SUMMARY = "GPS for the TicWatch E/S"
DESCRIPTION = "Runs MediaTek's GPS daemon (mnld), which the gps.mooneye.so HAL \
talks to, and lets the user session reach it. mooneye-gps-epo keeps \
MediaTek's GPS orbit predictions (EPO) up to date: it downloads them from \
MediaTek's server every few days (see /usr/bin/mtk-gps-epo for what that \
reveals). Leave it out with BAD_RECOMMENDATIONS, or run \
'mtk-gps-epo disable' on the watch, to keep the watch from contacting \
MediaTek; the GPS then takes longer to get a first fix."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://mnld.service \
           file://60-mtk-gps.rules \
           file://gps_xtra.ini \
           file://mtk-gps-epo \
           file://mtk-gps-epo.service \
           file://mtk-gps-epo.timer"
S = "${UNPACKDIR}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "mooneye"

inherit systemd

PACKAGES =+ "${PN}-epo"
SYSTEMD_PACKAGES = "${PN} ${PN}-epo"
SYSTEMD_SERVICE:${PN} = "mnld.service"
SYSTEMD_SERVICE:${PN}-epo = "mtk-gps-epo.timer"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 mnld.service mtk-gps-epo.service mtk-gps-epo.timer ${D}${systemd_system_unitdir}/
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 60-mtk-gps.rules ${D}${sysconfdir}/udev/rules.d/
    install -m 0644 gps_xtra.ini ${D}${sysconfdir}/
    install -d ${D}${bindir}
    install -m 0755 mtk-gps-epo ${D}${bindir}/
}

FILES:${PN} += "${sysconfdir}/udev ${sysconfdir}/gps_xtra.ini"
FILES:${PN}-epo = "${bindir}/mtk-gps-epo ${systemd_system_unitdir}/mtk-gps-epo.service ${systemd_system_unitdir}/mtk-gps-epo.timer"
RRECOMMENDS:${PN} += "${PN}-epo"
RDEPENDS:${PN}-epo += "wget"
