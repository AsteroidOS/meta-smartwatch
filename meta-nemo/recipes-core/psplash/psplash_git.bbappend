SPLASH_IMAGES = "file://psplash-img-400-220.gif;outsuffix=default"

SRC_URI:append:nemo = " file://0002-Disable-double-buffering.patch"

do_install:append:nemo() {
    install -d ${D}/usr/share/
    install -m 0755 ${UNPACKDIR}/psplash-img-400-220.gif ${D}/usr/share/psplash.gif
}
