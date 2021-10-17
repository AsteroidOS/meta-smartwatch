FILESEXTRAPATHS:prepend:smelt := "${THISDIR}/mce:"
SRC_URI:append:smelt = " file://20als-defaults.ini"

do_install:append:smelt() {
    cp ../20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
