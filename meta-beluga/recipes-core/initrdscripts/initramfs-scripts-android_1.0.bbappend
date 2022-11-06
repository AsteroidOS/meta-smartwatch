FILESEXTRAPATHS:prepend:beluga := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:beluga = "beluga"
SRC_URI:append:beluga = " file://init.machine.sh \
    file://ld.config.28.txt"

do_install:append:beluga() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
    install -m 0755 ${WORKDIR}/ld.config.28.txt ${D}/ld.config.28.txt
}

RDEPENDS:${PN}:append:beluga = " msm-fb-refresher"
FILES:${PN}:append:beluga = " /ld.config.28.txt"

