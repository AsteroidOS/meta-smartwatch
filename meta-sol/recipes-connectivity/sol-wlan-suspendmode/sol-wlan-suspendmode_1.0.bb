SUMMARY = "sol: WLAN suspend power-save mode while the display is off"
DESCRIPTION = "Sends the Android DHD private command SETSUSPENDMODE 1 at \
display-off and 0 at display-on, as Wear OS does, so the associated Broadcom \
chip listens at an extended DTIM interval with its packet filters engaged."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"

SRC_URI = "file://wl-privcmd.c \
           file://sol-wlan-suspendmode.sh \
           file://sol-wlan-suspendmode.service"
S = "${UNPACKDIR}"

inherit systemd

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -o wl-privcmd ${S}/wl-privcmd.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/wl-privcmd ${D}${bindir}/
    install -m 0755 ${S}/sol-wlan-suspendmode.sh ${D}${bindir}/
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/sol-wlan-suspendmode.service ${D}${systemd_system_unitdir}/
}

SYSTEMD_SERVICE:${PN} = "sol-wlan-suspendmode.service"
RDEPENDS:${PN} = "dbus-tools mce"
COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"
