FILESEXTRAPATHS:prepend:skipjack := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:skipjack = "skipjack"

RDEPENDS:${PN}:append:skipjack += "msm-fb-refresher"

SRC_URI:append:skipjack += " file://init.machine.sh"

do_install:append:skipjack() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

FILES:${PN} += "/init.machine"