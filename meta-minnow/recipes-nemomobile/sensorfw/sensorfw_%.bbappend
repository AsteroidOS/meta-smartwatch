FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append:minnow = " \
    file://90-minnow-wristgesture.rules \
    file://sensord-hybris.conf \
"

do_install:append:minnow() {
    cp ${UNPACKDIR}/sensord-hybris.conf ${D}/etc/sensorfw/primaryuse.conf
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/90-minnow-wristgesture.rules ${D}${sysconfdir}/udev/rules.d/
}
