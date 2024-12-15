do_install:append:sturgeon() {
    install -d ${D}/usr/share/
    install -m 0755 ${UNPACKDIR}/psplash-img-400-220.gif ${D}/usr/share/psplash.gif
}
