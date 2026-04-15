FILESEXTRAPATHS:prepend:rubyfish := "${THISDIR}/mce:"
SRC_URI:append:rubyfish = " file://20als-defaults.ini"

do_install:append:rubyfish() {
    cp ${UNPACKDIR}/20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
