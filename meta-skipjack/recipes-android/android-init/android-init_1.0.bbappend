FILESEXTRAPATHS_prepend_skipjack := "${THISDIR}/${PN}:"

SRC_URI_append_skipjack = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install_append_skipjack() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES_${PN} += "/nonplat* /plat*"
