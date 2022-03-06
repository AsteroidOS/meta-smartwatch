FILESEXTRAPATHS:prepend:catfish := "${THISDIR}/${PN}:"

SRC_URI:append:catfish = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:catfish() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES:${PN} += "/nonplat* /plat*"