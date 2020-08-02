FILESEXTRAPATHS_prepend_smelt := "${THISDIR}/mce:"
SRC_URI_append_smelt = " file://20als-defaults.ini"

do_install_append_smelt() {
    cp ../20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
