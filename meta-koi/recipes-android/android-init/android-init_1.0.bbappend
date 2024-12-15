FILESEXTRAPATHS:prepend:koi := "${THISDIR}/${PN}:"

SRC_URI:append:koi = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:koi() {
    install -m 0644 ${UNPACKDIR}/nonplat* ${D}/
    install -m 0644 ${UNPACKDIR}/plat* ${D}/
}

FILES:${PN} += "/nonplat* /plat*"
