FILESEXTRAPATHS:prepend:harmony := "${THISDIR}/mce:"
SRC_URI:append:harmony = " file://10mce.ini"

do_install:append:harmony() {
    cp ${UNPACKDIR}/10mce.ini ${D}/etc/mce/
}

FILESEXTRAPATHS:prepend:inharmony := "${THISDIR}/mce:"
SRC_URI:append:inharmony = " file://10mce.ini"

do_install:append:inharmony() {
    cp ${UNPACKDIR}/10mce.ini ${D}/etc/mce/
}
