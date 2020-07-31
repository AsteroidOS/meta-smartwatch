FILESEXTRAPATHS_prepend_sturgeon := "${THISDIR}/mce:"
SRC_URI_append_sturgeon = " file://20als-defaults.ini"

do_install_append_sturgeon() {
    cp ../20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
