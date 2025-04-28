FILESEXTRAPATHS:prepend:rubyfish := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:rubyfish = "rubyfish"
SRC_URI:append:rubyfish = " file://init.machine.sh \
    file://ld.config.28.txt"

do_install:append:rubyfish() {
    install -m 0755 ${WORKDIR}/init.machine.sh ${D}/init.machine
    install -m 0755 ${WORKDIR}/ld.config.28.txt ${D}/ld.config.28.txt
}

RDEPENDS:${PN}:append:rubyfish = " msm-fb-refresher"
FILES:${PN}:append:rubyfish = " /ld.config.28.txt"

