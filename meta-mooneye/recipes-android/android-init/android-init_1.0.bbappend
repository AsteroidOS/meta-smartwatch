FILESEXTRAPATHS:prepend:mooneye := "${THISDIR}/${PN}:"

SRC_URI:append:mooneye = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:mooneye() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
}

FILES:${PN}:append:mooneye = " /nonplat* /plat*"
