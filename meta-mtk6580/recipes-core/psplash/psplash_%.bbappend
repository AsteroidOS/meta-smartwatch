SPLASH_IMAGES = "file://psplash-img-400.png;outsuffix=default"

FILESEXTRAPATHS:prepend:harmony := "${THISDIR}/psplash:"
SRC_URI:append:harmony = " file://rotation"

do_install:append:harmony() {
    cp ${UNPACKDIR}/rotation ${D}/etc/rotation
}
