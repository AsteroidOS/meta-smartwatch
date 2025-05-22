FILESEXTRAPATHS:prepend:swift := "${THISDIR}/mce:"
SRC_URI:append:swift = " file://20als-defaults.ini"

do_install:append:swift() {
    cp ${UNPACKDIR}/20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
