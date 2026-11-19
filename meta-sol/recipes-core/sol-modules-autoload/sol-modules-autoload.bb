SUMMARY = "Auto-load post-rootfs sol kernel modules at boot"
DESCRIPTION = "Drops a systemd-modules-load.d config that loads the serial, SPI, \
input, WLAN/BT, QRTR transport and ALSA/ASoC modules shipped on the rootfs, a \
modprobe.d blacklist, and the udev rules that key a load on its provider's own \
add event. These cannot live in modules.load.sol because their .ko files are \
only reachable AFTER pivot_root to the rootfs (the VKB ramdisk holds only the \
boot-critical modules)."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-post-rootfs.conf \
           file://sol-blacklist.conf \
           file://70-sol-touchscreen.rules \
           file://71-sol-battery-gauge.rules \
           file://sol-wlan-wowlan.service"

S = "${UNPACKDIR}"

inherit systemd

do_install() {
    install -d -m 0755 ${D}${sysconfdir}/modules-load.d
    install -m 0644 ${UNPACKDIR}/sol-post-rootfs.conf ${D}${sysconfdir}/modules-load.d/sol-post-rootfs.conf
    install -d -m 0755 ${D}${sysconfdir}/modprobe.d
    install -m 0644 ${UNPACKDIR}/sol-blacklist.conf ${D}${sysconfdir}/modprobe.d/sol-blacklist.conf
    install -d -m 0755 ${D}${nonarch_base_libdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/70-sol-touchscreen.rules ${D}${nonarch_base_libdir}/udev/rules.d/70-sol-touchscreen.rules
    install -m 0644 ${UNPACKDIR}/71-sol-battery-gauge.rules ${D}${nonarch_base_libdir}/udev/rules.d/71-sol-battery-gauge.rules

    install -d -m 0755 ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/sol-wlan-wowlan.service ${D}${systemd_system_unitdir}/sol-wlan-wowlan.service
}

FILES:${PN} = "${sysconfdir}/modules-load.d/sol-post-rootfs.conf \
               ${sysconfdir}/modprobe.d/sol-blacklist.conf \
               ${nonarch_base_libdir}/udev/rules.d/70-sol-touchscreen.rules \
               ${nonarch_base_libdir}/udev/rules.d/71-sol-battery-gauge.rules \
               ${systemd_system_unitdir}/sol-wlan-wowlan.service"

RDEPENDS:${PN} = "linux-sol-modules iw"

SYSTEMD_SERVICE:${PN} = "sol-wlan-wowlan.service"
