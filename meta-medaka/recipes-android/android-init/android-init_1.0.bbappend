FILESEXTRAPATHS:prepend:medaka := "${THISDIR}/${PN}:"

SRC_URI:append:medaka = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:medaka() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES:${PN}:append:medaka = " /nonplat* /plat*"
