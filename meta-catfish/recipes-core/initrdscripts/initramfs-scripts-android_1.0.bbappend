FILESEXTRAPATHS_prepend_sawfish := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE_sawfish = "sawfish"

SRC_URI_append_sawfish += " file://init.machine.sh"

do_install_append_sawfish() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES_${PN} += "/init.machine"