FILESEXTRAPATHS_prepend_mooneye := "${THISDIR}/${PN}:"

SRC_URI_append_mooneye = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install_append_mooneye() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES_${PN} += "/nonplat* /plat*"
