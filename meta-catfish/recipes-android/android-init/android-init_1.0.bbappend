FILESEXTRAPATHS:prepend:catfish := "${THISDIR}/${PN}:"

SRC_URI:append:catfish = " file://nonplat_property_contexts \
    file://plat_property_contexts \
    file://default.prop"

do_install:append:catfish() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
    install -m 0644 ${UNPACKDIR}/default.prop ${D}/
}

FILES:${PN} += "/nonplat* /plat* /default.prop"