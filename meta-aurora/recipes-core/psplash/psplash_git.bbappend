SRC_URI:append:aurora = " file://0002-Disable-double-buffering.patch"

do_install:append:aurora() {
    install -d ${D}/usr/share/
    install -m 0755 ${UNPACKDIR}/psplash-img-400-220.gif ${D}/usr/share/psplash.gif
}
