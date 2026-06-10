FILESEXTRAPATHS:prepend:aurora := "${THISDIR}/files:"

# Aurora has its own configfs gadget setup -- see init_gfs.service and
# aurora-udc-bind.service in the usb-moded bbappend. The upstream drop-in
# shipped by this recipe (10-adbd-configfs.conf) would create a competing `adb`
# gadget and bind the UDC to it, breaking rootfs adb. Layer a higher-priority
# drop-in that clears its hooks.
SRC_URI:append:aurora = " file://99-aurora-no-configfs.conf"

do_install:append:aurora() {
    install -d ${D}${systemd_unitdir}/system/android-tools-adbd.service.d
    install -m 0644 ${UNPACKDIR}/99-aurora-no-configfs.conf \
        ${D}${systemd_unitdir}/system/android-tools-adbd.service.d/99-aurora-no-configfs.conf
}

FILES:${PN}:append:aurora = " \
    ${systemd_unitdir}/system/android-tools-adbd.service.d/99-aurora-no-configfs.conf"
