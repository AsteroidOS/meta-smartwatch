FILESEXTRAPATHS_prepend_harmony := "${THISDIR}/mce:"
SRC_URI_append_harmony = " file://10mce.ini"

do_install_append_harmony() {
    cp ${WORKDIR}/10mce.ini ${D}/etc/mce/
}

FILESEXTRAPATHS_prepend_inharmony := "${THISDIR}/mce:"
SRC_URI_append_inharmony = " file://10mce.ini"

do_install_append_inharmony() {
    cp ${WORKDIR}/10mce.ini ${D}/etc/mce/
}
