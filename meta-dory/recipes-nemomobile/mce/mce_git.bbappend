FILESEXTRAPATHS:prepend:dory := "${THISDIR}/mce:"
SRC_URI:append:dory = " file://20als-defaults.ini"

do_install:append:dory() {
    cp ${UNPACKDIR}/20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
