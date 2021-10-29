FILESEXTRAPATHS:prepend:skipjack := "${THISDIR}/${PN}:"

SRC_URI:append:skipjack = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:skipjack() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES:${PN} += "/nonplat* /plat*"
