FILESEXTRAPATHS:prepend:hoki := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:hoki = "hoki"
SRC_URI:append:hoki = " file://init.machine.sh \
    file://ld.config.28.txt"

do_install:append:hoki() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
    install -m 0755 ${WORKDIR}/ld.config.28.txt ${D}/ld.config.28.txt
}

RDEPENDS:${PN}:append:hoki = " msm-fb-refresher"
FILES:${PN}:append:hoki = " /ld.config.28.txt"

