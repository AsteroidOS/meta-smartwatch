FILESEXTRAPATHS:prepend:hoki := "${THISDIR}/${PN}:"
SRC_URI:append:hoki = " file://libncicore.conf"

do_install:append:hoki() {
    install -d ${D}${sysconfdir}
    install -m 0666 ${WORKDIR}/libncicore.conf ${D}${sysconfdir}/libncicore.conf
}
