SPLASH_IMAGES = "file://psplash-img-400.png;outsuffix=default"

FILESEXTRAPATHS_prepend := "${THISDIR}/psplash:"
SRC_URI += " file://rotation"
do_install_append() {
    cp ${WORKDIR}/rotation ${D}/etc/rotation
}
