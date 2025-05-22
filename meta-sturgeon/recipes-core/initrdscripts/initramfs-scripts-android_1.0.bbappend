FILESEXTRAPATHS:prepend:sturgeon := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:sturgeon = "sturgeon"

SRC_URI:append:sturgeon = " file://init.machine.sh"

do_install:append:sturgeon() {
    install -m 0755 ${UNPACKDIR}/init.machine.sh ${D}/init.machine
}

FILES:${PN} += "/init.machine"
