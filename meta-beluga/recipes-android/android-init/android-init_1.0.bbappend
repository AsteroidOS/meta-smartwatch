FILESEXTRAPATHS:prepend:beluga := "${THISDIR}/${PN}:"

SRC_URI:append:beluga = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:beluga() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
}

FILES:${PN}:append:beluga = " /nonplat* /plat*"
