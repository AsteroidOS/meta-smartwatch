FILESEXTRAPATHS:prepend:skipjack := "${THISDIR}/${PN}:"

SRC_URI:append:skipjack = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:skipjack() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
}

FILES:${PN}:append:skipjack = " /nonplat* /plat*"
