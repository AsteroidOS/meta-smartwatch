SRC_URI:append:ray += " file://0002-Disable-double-buffering.patch"

do_install:append:ray() {
    install -d ${D}/usr/share/
    install -m 0755 ${WORKDIR}/psplash-img-400-220.gif ${D}/usr/share/psplash.gif
}

SRC_URI:append:firefish += " file://0002-Disable-double-buffering.patch"

do_install:append:firefish() {
    install -d ${D}/usr/share/
    install -m 0755 ${WORKDIR}/psplash-img-400-220.gif ${D}/usr/share/psplash.gif
}
