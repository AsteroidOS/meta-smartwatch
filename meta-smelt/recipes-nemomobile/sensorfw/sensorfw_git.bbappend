FILESEXTRAPATHS_prepend_smelt := "${THISDIR}/sensorfw:"
SRC_URI_append_smelt = " file://sensord-hybris.conf"

do_install_append() {
    cp ../sensord-hybris.conf ${D}/etc/sensorfw/primaryuse.conf
}

DEPENDS_append_smelt = " libhybris "
