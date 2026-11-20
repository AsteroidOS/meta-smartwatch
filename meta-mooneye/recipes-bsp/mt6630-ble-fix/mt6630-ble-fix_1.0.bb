SUMMARY = "Patch the MT6630 combo firmware at boot so LE slave connections work"
DESCRIPTION = "The watch builds of MediaTek's MT6630 patch abort every LE slave connection event while the chip \
CPU runs below 80 MHz. At boot this copies the stock patch to /run, changes one instruction, verifies the \
result by md5 and bind-mounts it over the original before Bluetooth powers on. The stock file is untouched."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://mt6630-ble-fix file://mt6630-ble-fix.service"
S = "${UNPACKDIR}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "mooneye"

inherit systemd

do_install() {
    install -D -m 0755 ${S}/mt6630-ble-fix ${D}${sbindir}/mt6630-ble-fix
    install -D -m 0644 ${S}/mt6630-ble-fix.service ${D}${systemd_system_unitdir}/mt6630-ble-fix.service
}

SYSTEMD_SERVICE:${PN} = "mt6630-ble-fix.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
