FILESEXTRAPATHS_prepend_lenok := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE_lenok = "lenok"

SRC_URI_append_lenok += " file://init.machine.sh"

do_install_append_lenok() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES_${PN} += "/init.machine"
