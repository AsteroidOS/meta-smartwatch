SUMMARY = "sol: give wlan0 the factory MAC from the device tree, every boot"
DESCRIPTION = "sol's wlan0 comes up with a different Broadcom test-OUI address on \
almost every load, and connman's saved-network identity embeds the interface MAC \
-- so WiFi silently never auto-connects after a reboot. This applies the address \
the bootloader publishes as chosen/config/wlan_mac1, which is the same place the \
BT address comes from."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

# rfkill is needed to lift the soft block before the interface can come up.
RDEPENDS:${PN} = "rfkill"

SRC_URI = "file://sol-wlan-mac.sh \
           file://sol-wlan-mac.service"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-wlan-mac.sh ${D}${libexecdir}/sol-wlan-mac.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-wlan-mac.service \
        ${D}${systemd_unitdir}/system/sol-wlan-mac.service
}

SYSTEMD_SERVICE:${PN} = "sol-wlan-mac.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "${libexecdir}/sol-wlan-mac.sh \
               ${systemd_unitdir}/system/sol-wlan-mac.service"
