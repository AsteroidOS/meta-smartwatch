SPLASH_IMAGES = "file://psplash-img-400.png;outsuffix=default"

FILESEXTRAPATHS_prepend_harmony := "${THISDIR}/psplash:"
SRC_URI_append_harmony = " file://rotation"

do_install_append_harmony() {
    cp ${WORKDIR}/rotation ${D}/etc/rotation
}
