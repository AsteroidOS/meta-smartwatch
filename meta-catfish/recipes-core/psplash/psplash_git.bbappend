SRC_URI:append:catfish = " file://0002-Disable-double-buffering.patch"

do_install:append:catfish() {
    install -d ${D}/usr/share/
    install -m 0755 ${UNPACKDIR}/psplash-img-400-220.gif ${D}/usr/share/psplash.gif
}