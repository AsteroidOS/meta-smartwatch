FILESEXTRAPATHS:prepend:medaka := "${THISDIR}/${PN}:"

SRC_URI:append:medaka = " file://nonplat_property_contexts \
    file://plat_property_contexts \
    file://macaddr-generator"

do_install:append:medaka() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/

    install -m 0755 -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/macaddr-generator ${D}${bindir}/ 
}

FILES:${PN}:append:medaka = " /nonplat* /plat* ${bindir}/macaddr-generator"
