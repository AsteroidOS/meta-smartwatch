FILESEXTRAPATHS:prepend:smelt := "${THISDIR}/sensorfw:"
SRC_URI:append:smelt = " file://sensord-hybris.conf"

do_install:append:smelt() {
    cp ../sensord-hybris.conf ${D}/etc/sensorfw/primaryuse.conf
}

DEPENDS:append:smelt = " libhybris "
