FILESEXTRAPATHS_prepend_skipjack := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE_skipjack = "skipjack"

RDEPENDS_${PN}_append_skipjack += "msm-fb-refresher"

SRC_URI_append_skipjack += " file://init.machine.sh"

do_install_append_skipjack() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES_${PN} += "/init.machine"