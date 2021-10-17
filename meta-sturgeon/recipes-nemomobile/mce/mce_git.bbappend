FILESEXTRAPATHS:prepend:sturgeon := "${THISDIR}/mce:"
SRC_URI:append:sturgeon = " file://20als-defaults.ini"

do_install:append:sturgeon() {
    cp ../20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
