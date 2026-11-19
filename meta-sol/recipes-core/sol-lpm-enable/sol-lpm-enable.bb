SUMMARY = "Clear the qcom-lpm governor's sleep_disabled boot default"
DESCRIPTION = "The qcom_lpm cpuidle governor hard-codes sleep_disabled=true \
at load (Qualcomm's boot-stability default; stock userspace clears it after \
boot). This oneshot writes 0 to the sysfs knob after modules-load so cluster \
and system low-power modes actually engage. Companion to the qcom_lpm entry \
in sol-modules-autoload."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-lpm-enable.service"

S = "${UNPACKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "sol-lpm-enable.service"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/sol-lpm-enable.service ${D}${systemd_system_unitdir}/sol-lpm-enable.service
}

FILES:${PN} = "${systemd_system_unitdir}/sol-lpm-enable.service"

RDEPENDS:${PN} = "sol-modules-autoload"
