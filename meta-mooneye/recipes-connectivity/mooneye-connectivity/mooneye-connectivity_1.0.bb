DESCRIPTION = "NVRAM restore, Wi-Fi enabler and Bluetooth workarounds for the MediaTek MT6630 in the TicWatch E/S"
PR = "r0"
SRC_URI = "file://mtk-wifi-nvram.c \
           file://mtk-wifi-wowlan.c \
           file://mtk-nvram-restore \
           file://mtk-nvram-restore.service \
           file://mtk-wifi-enabler \
           file://mtk-wifi-enabler.service \
           file://mtk-bt-workarounds \
           file://mtk-bt-workarounds.service"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"
S = "${UNPACKDIR}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "mooneye"

RDEPENDS:${PN} = "bluez5 connman-client"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -o mtk-wifi-nvram mtk-wifi-nvram.c
    ${CC} ${CFLAGS} ${LDFLAGS} -o mtk-wifi-wowlan mtk-wifi-wowlan.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 mtk-wifi-nvram ${D}${bindir}
    install -m 0755 mtk-wifi-wowlan ${D}${bindir}
    install -m 0755 mtk-nvram-restore ${D}${bindir}
    install -m 0755 mtk-wifi-enabler ${D}${bindir}
    install -m 0755 mtk-bt-workarounds ${D}${bindir}

    install -d ${D}/etc/systemd/system/multi-user.target.wants/
    install -m 0644 mtk-nvram-restore.service ${D}/etc/systemd/system/
    install -m 0644 mtk-wifi-enabler.service ${D}/etc/systemd/system/
    install -m 0644 mtk-bt-workarounds.service ${D}/etc/systemd/system/
    ln -s ../mtk-nvram-restore.service ${D}/etc/systemd/system/multi-user.target.wants/mtk-nvram-restore.service
    ln -s ../mtk-wifi-enabler.service ${D}/etc/systemd/system/multi-user.target.wants/mtk-wifi-enabler.service
    ln -s ../mtk-bt-workarounds.service ${D}/etc/systemd/system/multi-user.target.wants/mtk-bt-workarounds.service

    # systemd-resolved asserts ("ifindex > 0") on a link message the MediaTek
    # WLAN driver emits when wlan0 is created. Its restart then stops connman
    # (connman.service conflicts with systemd-resolved.service), which takes
    # usb0 down with it. connman already provides DNS, so mask resolved here.
    ln -s /dev/null ${D}/etc/systemd/system/systemd-resolved.service
}

FILES:${PN} = "${bindir} /etc/systemd/system"
