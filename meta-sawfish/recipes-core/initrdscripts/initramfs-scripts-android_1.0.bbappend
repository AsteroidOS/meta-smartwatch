FILESEXTRAPATHS:prepend:sawfish := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:sawfish = "sawfish"

SRC_URI:append:sawfish = " file://init.machine.sh"

do_install:append:sawfish() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES:${PN} += "/init.machine"