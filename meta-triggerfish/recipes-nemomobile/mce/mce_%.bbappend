FILESEXTRAPATHS:prepend:triggerfish := "${THISDIR}/mce:"
SRC_URI:append:triggerfish = " file://10mce.ini"

do_install:append:triggerfish() {
    cp ${WORKDIR}/10mce.ini ${D}/etc/mce/
}