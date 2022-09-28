FILESEXTRAPATHS:prepend:koi := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:koi = "koi"

SRC_URI:append:koi = " file://init.machine.sh"

do_install:append:koi() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES:${PN} += "/init.machine"