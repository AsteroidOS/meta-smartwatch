FILESEXTRAPATHS_prepend_sturgeon := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE_sturgeon = "sturgeon"

SRC_URI_append_sturgeon += " file://init.machine.sh"

do_install_append_sturgeon() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES_${PN} += "/init.machine"
