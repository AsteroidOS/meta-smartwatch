FILESEXTRAPATHS:prepend:ray := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:ray = "ray"

RDEPENDS:${PN}:append:ray += "msm-fb-refresher"

SRC_URI:append:ray += " file://init.machine.sh"

do_install:append:ray() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES:${PN} += "/init.machine"