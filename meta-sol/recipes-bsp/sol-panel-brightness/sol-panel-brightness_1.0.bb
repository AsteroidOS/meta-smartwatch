SUMMARY = "sol: advertise the panel's real brightness range to userspace"
DESCRIPTION = "sol's panel packs normal brightness into the low 16 bits of the \
backlight value and AOD into the high 16, so its advertised max_brightness of \
0xFF01FF is not a linear maximum. Anything scaling a percentage against it -- mce \
does -- lands most of the value in the AOD field, which is why brightness did not \
follow the setting. This bind-mounts the normal-brightness maximum over the sysfs \
attribute before mce reads it."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://sol-panel-brightness.sh \
           file://sol-panel-brightness.service"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-panel-brightness.sh ${D}${libexecdir}/sol-panel-brightness.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-panel-brightness.service \
        ${D}${systemd_unitdir}/system/sol-panel-brightness.service
}

SYSTEMD_SERVICE:${PN} = "sol-panel-brightness.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "${libexecdir}/sol-panel-brightness.sh \
               ${systemd_unitdir}/system/sol-panel-brightness.service"
