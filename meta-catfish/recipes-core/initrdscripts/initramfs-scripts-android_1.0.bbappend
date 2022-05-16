FILESEXTRAPATHS:prepend:catfish := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:catfish = "catfish"

PR = "r1"

RDEPENDS:${PN}:append:catfish += "msm-fb-refresher"

SRC_URI:append:catfish = " file://init.machine.sh"

do_install:append:catfish() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES:${PN} += "/init.machine"
