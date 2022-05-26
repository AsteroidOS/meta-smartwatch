FILESEXTRAPATHS:prepend:beluga := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:beluga = "beluga"
SRC_URI:append:beluga = " file://init.machine.sh"

do_install:append:beluga() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
}

RDEPENDS:${PN}:append:beluga = " msm-fb-refresher"
