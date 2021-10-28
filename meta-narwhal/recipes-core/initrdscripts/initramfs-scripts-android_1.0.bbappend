FILESEXTRAPATHS_prepend_ray := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE_ray = "ray"

RDEPENDS_${PN}_append_ray += "msm-fb-refresher"

SRC_URI_append_ray += " file://init.machine.sh"

do_install_append_ray() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES_${PN} += "/init.machine"