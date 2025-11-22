FILESEXTRAPATHS:prepend:ray := "${THISDIR}/${PN}:"

SRC_URI:append:ray = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:ray() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
}

FILES:${PN}:append:ray = " /nonplat* /plat*"

FILESEXTRAPATHS:prepend:firefish := "${THISDIR}/${PN}:"

SRC_URI:append:firefish = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:firefish() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
}

FILES:${PN}:append:firefish = " /nonplat* /plat*"