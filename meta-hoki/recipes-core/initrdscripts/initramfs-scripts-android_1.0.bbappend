FILESEXTRAPATHS:prepend:hoki := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:hoki = "hoki"
SRC_URI:append:hoki = " file://init.machine.sh"

do_install:append:hoki() {
    install -m 0755 ${UNPACKDIR}/init.machine.sh ${D}/init.machine
}

RDEPENDS:${PN}:append:hoki = " msm-fb-refresher"

