FILESEXTRAPATHS:prepend:catfish := "${THISDIR}/mce:"
SRC_URI:append:catfish = " file://10mce.ini"

do_install:append:catfish() {
    cp ${UNPACKDIR}/10mce.ini ${D}/etc/mce/
}