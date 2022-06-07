FILESEXTRAPATHS:prepend:minnow := "${THISDIR}/mce:"
SRC_URI:append:minnow = " file://20als-defaults.ini"

do_install:append:minnow() {
    cp ../20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
