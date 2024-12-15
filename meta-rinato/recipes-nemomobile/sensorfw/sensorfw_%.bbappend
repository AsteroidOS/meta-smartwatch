FILESEXTRAPATHS:prepend:rinato := "${THISDIR}/files:"

SRC_URI:append:rinato = " file://primaryuse.conf "

do_install:append:rinato() {
    cp ${UNPACKDIR}/primaryuse.conf ${D}/etc/sensorfw/primaryuse.conf
}
