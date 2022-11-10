FILESEXTRAPATHS:prepend:triggerfish := "${THISDIR}/${PN}:"

SRC_URI:append:triggerfish = " file://nonplat_property_contexts \
    file://plat_property_contexts \
    file://default.prop"

do_install:append:triggerfish() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
    install -m 0644 ${WORKDIR}/default.prop ${D}/
}

FILES:${PN} += "/nonplat* /plat* /default.prop"