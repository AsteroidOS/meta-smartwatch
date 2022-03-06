FILESEXTRAPATHS:prepend:ray := "${THISDIR}/${PN}:"

SRC_URI:append:ray = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:ray() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES:${PN} += "/nonplat* /plat*"

FILESEXTRAPATHS:prepend:firefish := "${THISDIR}/${PN}:"

SRC_URI:append:firefish = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:firefish() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES:${PN} += "/nonplat* /plat*"