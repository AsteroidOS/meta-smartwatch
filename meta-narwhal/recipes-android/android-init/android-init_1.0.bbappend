FILESEXTRAPATHS:prepend:narwhal := "${THISDIR}/${PN}:"

SRC_URI:append:narwhal = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:narwhal() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES:${PN} += "/nonplat* /plat*"
