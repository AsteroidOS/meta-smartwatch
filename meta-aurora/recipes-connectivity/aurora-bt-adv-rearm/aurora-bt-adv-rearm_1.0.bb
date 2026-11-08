SUMMARY = "aurora: re-register BLE advertising after Bluetooth is switched on"
DESCRIPTION = "Switching Bluetooth off and on leaves the watch invisible: the \
HCI Reset at power-off wipes the controller's advertising set, the kernel's \
power-on path gets 0x42 Unknown Advertising Identifier for handle 1, and the set \
is never created again. This restarts asteroid-btsyncd whenever the adapter's \
Powered property turns true, which registers the advertisement anew. \
Device-scoped workaround for a fault in shared code (seen on sol and aurora)."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "aurora"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://aurora-bt-adv-rearm.sh \
           file://aurora-bt-adv-rearm.service"

S = "${UNPACKDIR}"

# dbus-monitor is packaged in dbus-tools.
RDEPENDS:${PN} = "dbus-tools bluez5"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/aurora-bt-adv-rearm.sh ${D}${bindir}/aurora-bt-adv-rearm.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/aurora-bt-adv-rearm.service \
        ${D}${systemd_unitdir}/system/aurora-bt-adv-rearm.service
}

SYSTEMD_SERVICE:${PN} = "aurora-bt-adv-rearm.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "${bindir}/aurora-bt-adv-rearm.sh \
               ${systemd_unitdir}/system/aurora-bt-adv-rearm.service"
