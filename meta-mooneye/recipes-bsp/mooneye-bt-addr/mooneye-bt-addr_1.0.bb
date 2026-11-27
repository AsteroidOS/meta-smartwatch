SUMMARY = "Give mooneye's MT6630 Bluetooth its factory address and RF calibration"
DESCRIPTION = "The HCI_STP driver falls back to MediaTek's default address 00:00:46:66:20:01 and generic radio \
values when /data/BT_Addr is missing. At boot this copies the factory record from the nvram partition there, \
before the combo chip powers on. An existing non-default file is left alone."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://mooneye-bt-addr file://mooneye-bt-addr.service"
S = "${UNPACKDIR}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "mooneye"

inherit systemd

do_install() {
    install -D -m 0755 ${S}/mooneye-bt-addr ${D}${sbindir}/mooneye-bt-addr
    install -D -m 0644 ${S}/mooneye-bt-addr.service ${D}${systemd_system_unitdir}/mooneye-bt-addr.service
}

SYSTEMD_SERVICE:${PN} = "mooneye-bt-addr.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
