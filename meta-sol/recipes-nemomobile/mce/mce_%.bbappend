FILESEXTRAPATHS:prepend:sol := "${THISDIR}/mce:"

SRC_URI:append:sol = " file://50-sol.conf"

do_install:append:sol() {
    install -d ${D}${sysconfdir}/mce
    install -m 0644 ${UNPACKDIR}/50-sol.conf ${D}${sysconfdir}/mce/50-sol.conf
}
