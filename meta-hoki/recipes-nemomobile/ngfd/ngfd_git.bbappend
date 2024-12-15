FILESEXTRAPATHS:prepend:hoki := "${THISDIR}/ngfd:"
SRC_URI:append:hoki = "file://51-ffmemless.ini"

do_install:append:hoki() {
    install -m 0644 ${UNPACKDIR}/51-ffmemless.ini ${D}/usr/share/ngfd/plugins.d/
}