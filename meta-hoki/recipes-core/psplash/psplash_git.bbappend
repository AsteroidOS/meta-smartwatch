SRC_URI:append:hoki = " file://0002-Disable-double-buffering.patch"

do_install:append:hoki() {
    install -d ${D}/usr/share/
    install -m 0755 ${WORKDIR}/psplash-img-400-220.gif ${D}/usr/share/psplash.gif
}
