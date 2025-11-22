FILESEXTRAPATHS:prepend:pike := "${THISDIR}/${PN}:"

SRC_URI:append:pike = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:pike() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
}

FILES:${PN}:append:pike = " /nonplat* /plat*"
