FILESEXTRAPATHS:prepend:triggerfish := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:triggerfish = "triggerfish"
SRC_URI:append:triggerfish = " file://init.machine.sh \
    file://ld.config.28.txt"

do_install:append:triggerfish() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
    install -m 0755 ${WORKDIR}/ld.config.28.txt ${D}/ld.config.28.txt
}

RDEPENDS:${PN}:append:triggerfish = " msm-fb-refresher"
FILES:${PN}:append:triggerfish = " /ld.config.28.txt"
