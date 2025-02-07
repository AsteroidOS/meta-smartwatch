FILESEXTRAPATHS:prepend:rubyfish := "${THISDIR}/${PN}:"

SRC_URI:append:rubyfish = " file://nonplat_property_contexts \
    file://plat_property_contexts"

do_install:append:rubyfish() {
    install -m 0644 ${WORKDIR}/nonplat* ${D}/
    install -m 0644 ${WORKDIR}/plat* ${D}/
}

FILES:${PN}:append:rubyfish = " /nonplat* /plat*"
