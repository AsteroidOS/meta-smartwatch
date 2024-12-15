do_install:append:sparrow() {
    install -d ${D}/usr/share/
    install -m 0755 ${UNPACKDIR}/psplash-img-320-176.gif ${D}/usr/share/psplash.gif
}
