FILESEXTRAPATHS:prepend:medaka := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:medaka = "medaka"
SRC_URI:append:medaka = " file://init.machine.sh \
    file://ld.config.28.txt"

do_install:append:medaka() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
    install -m 0755 ${WORKDIR}/ld.config.28.txt ${D}/ld.config.28.txt
}

RDEPENDS:${PN}:append:medaka = " msm-fb-refresher"
FILES:${PN}:append:medaka = " /ld.config.28.txt"

