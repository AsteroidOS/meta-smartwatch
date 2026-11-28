SUMMARY = "sol BRING-UP: bind the adb gadget without usb-moded"
DESCRIPTION = "Brings adb up after switch_root by doing the one thing usb-moded's \
adb_mode does that we still need, and masks usb-moded so the two cannot fight \
over configfs. REMOVE THIS once usb-moded works on sol."

# On sol, usb-moded detects configfs and then stops:
#     usb_moded: CONFIGFS detected
#     usb_moded: udev io watch disabled     <- log_crit, G_IO_ERR/HUP on the
#                                              udev monitor fd (usb_moded-udev.c)
# It never sets a mode, so the gadget stays the empty g1 skeleton init_gfs
# staged (18d1:0afe, mass storage only) and there is no adb after switch_root.
# Not yet diagnosed. Ruled out: sol-defaults.ini (it is installed, mode=adb_mode),
# the rndis mkdir failure (mode enumeration only), udevd (enabled), the
# FFS_ACTIVE/ep1 wait patch (applied), and USB_MODED_ARGS=--fallback.
#
# usb-moded must be masked: its configfs_init() zeroes UDC and wipes
# configs/b.1, destroying the gadget this unit builds. With it masked there is
# no appsync to start adbd, so adbd is enabled directly.
#
# sol-bringup-adb.sh binds the UDC once at boot. With usb-moded masked nothing
# else tracks cable state, so after a VBUS loss the gadget never re-enumerates.
# sol-usb-recover.service watches /sys/class/udc/<udc>/state and rebinds; see
# sol-usb-recover.sh for why it is slow to act.

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://sol-bringup-adb.sh \
           file://sol-bringup-adb.service \
           file://sol-usb-recover.sh \
           file://sol-usb-recover.service \
           file://sol-usb-network.sh \
           file://sol-usb-network.service"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "android-tools-adbd"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/sol-bringup-adb.sh ${D}${bindir}/sol-bringup-adb.sh

    install -m 0755 ${UNPACKDIR}/sol-usb-recover.sh ${D}${bindir}/sol-usb-recover.sh
    install -m 0755 ${UNPACKDIR}/sol-usb-network.sh ${D}${bindir}/sol-usb-network.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-bringup-adb.service \
        ${D}${systemd_unitdir}/system/sol-bringup-adb.service
    install -m 0644 ${UNPACKDIR}/sol-usb-recover.service \
        ${D}${systemd_unitdir}/system/sol-usb-recover.service
    install -m 0644 ${UNPACKDIR}/sol-usb-network.service \
        ${D}${systemd_unitdir}/system/sol-usb-network.service

    # Mask usb-moded here so the mask and this unit ship together; either
    # alone leaves no adb.
    install -d ${D}${sysconfdir}/systemd/system
    ln -sf /dev/null ${D}${sysconfdir}/systemd/system/usb-moded.service

    # android-tools-adbd is preset-disabled by 10-sol-android-tools.preset.
    # With usb-moded masked, enable it explicitly or nothing starts adbd.
    install -d ${D}${sysconfdir}/systemd/system/multi-user.target.wants
    ln -sf ${systemd_unitdir}/system/android-tools-adbd.service \
        ${D}${sysconfdir}/systemd/system/multi-user.target.wants/android-tools-adbd.service
}

SYSTEMD_SERVICE:${PN} = "sol-bringup-adb.service sol-usb-recover.service sol-usb-network.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "${bindir}/sol-bringup-adb.sh \
               ${bindir}/sol-usb-recover.sh \
               ${bindir}/sol-usb-network.sh \
               ${systemd_unitdir}/system/sol-bringup-adb.service \
               ${systemd_unitdir}/system/sol-usb-recover.service \
               ${systemd_unitdir}/system/sol-usb-network.service \
               ${sysconfdir}/systemd/system/usb-moded.service \
               ${sysconfdir}/systemd/system/multi-user.target.wants/android-tools-adbd.service"
