SUMMARY = "Attach the WCN3988 BT over UART (btattach -P qca) for dace"
DESCRIPTION = "The T5 BT (WCN3988) goes over /dev/ttyHS0 with the kernel QCA \
line (hci_uart + btqca). The btqca driver downloads the firmware \
qca/apbtfw*.tlv + qca/apnv*.bin (dace-bt-firmware recipe). The WCN3988 soc_type \
is forced on the LDISC path with wcn3988-bt.patch. Service before \
bluetooth.service."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "dace"

SRC_URI = "file://dace-btattach.service"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/dace-btattach.service \
        ${D}${systemd_unitdir}/system/dace-btattach.service
}

FILES:${PN} = "${systemd_unitdir}/system/dace-btattach.service"

inherit systemd
SYSTEMD_SERVICE:${PN} = "dace-btattach.service"
# For safety it is not auto-started: the historical attach reset the SoC.
# Try it by hand (systemctl start dace-btattach) and, if it works, set "enable".
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

RDEPENDS:${PN} += "bluez5"
