FILESEXTRAPATHS:prepend:dace := "${THISDIR}/files:"

# Dace has its own configfs gadget setup -- see init_gfs.service and
# dace-udc-bind.service in the usb-moded bbappend. The upstream drop-in
# shipped by this recipe (10-adbd-configfs.conf) would create a competing `adb`
# gadget and bind the UDC to it, breaking rootfs adb. Layer a higher-priority
# drop-in that clears its hooks.
SRC_URI:append:dace = " file://99-dace-no-configfs.conf"

do_install:append:dace() {
    install -d ${D}${systemd_unitdir}/system/android-tools-adbd.service.d
    install -m 0644 ${UNPACKDIR}/99-dace-no-configfs.conf \
        ${D}${systemd_unitdir}/system/android-tools-adbd.service.d/99-dace-no-configfs.conf
}

FILES:${PN}:append:dace = " \
    ${systemd_unitdir}/system/android-tools-adbd.service.d/99-dace-no-configfs.conf"
