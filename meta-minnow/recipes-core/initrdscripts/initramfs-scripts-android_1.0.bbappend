FILESEXTRAPATHS:prepend:minnow := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:minnow = "minnow"

SRC_URI:append:minnow = " file://init.machine.sh"

do_install:append:minnow() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES:${PN} += "/init.machine"
