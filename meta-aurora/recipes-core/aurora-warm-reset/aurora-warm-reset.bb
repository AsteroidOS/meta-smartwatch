SUMMARY = "Keep a kernel crash on aurora diagnosable and self-recovering"
DESCRIPTION = "Makes resets warm so the ramoops region survives into the next \
boot's pstore, keeps the Qualcomm download-mode cookie unarmed so a panic or \
watchdog bite reboots the watch instead of parking it in USB ramdump mode, and \
raises the console loglevel so the ramoops console keeps its info lines."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"

SRC_URI = "file://aurora-warm-reset.sh file://aurora-warm-reset.service"
S = "${UNPACKDIR}"

inherit systemd

do_install() {
    install -Dm755 ${UNPACKDIR}/aurora-warm-reset.sh ${D}${bindir}/aurora-warm-reset.sh
    install -Dm644 ${UNPACKDIR}/aurora-warm-reset.service ${D}${systemd_system_unitdir}/aurora-warm-reset.service
}

SYSTEMD_SERVICE:${PN} = "aurora-warm-reset.service"
