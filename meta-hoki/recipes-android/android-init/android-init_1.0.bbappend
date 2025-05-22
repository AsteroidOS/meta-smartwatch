FILESEXTRAPATHS:prepend:hoki := "${THISDIR}/${PN}:"

SRC_URI:append:hoki = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:hoki() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
}

FILES:${PN}:append:hoki = " /nonplat* /plat*"
