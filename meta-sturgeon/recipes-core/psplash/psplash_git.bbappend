do_install:append:sturgeon() {
    install -d ${D}/usr/share/
    install -m 0755 ${WORKDIR}/psplash-img-400-220.gif ${D}/usr/share/psplash.gif
}
