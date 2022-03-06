FILESEXTRAPATHS:prepend:lenok := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:lenok = "lenok"

SRC_URI:append:lenok = " file://init.machine.sh"

do_install:append:lenok() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES:${PN} += "/init.machine"
