FILESEXTRAPATHS:prepend:emulator := "${THISDIR}/${PN}:"
SRC_URI:prepend:emulator = " file://kms.json file://default.conf "

do_install:append:emulator() {
        install -m 0644 ${UNPACKDIR}/kms.json ${D}/var/lib/environment/compositor/
        install -m 0644 ${UNPACKDIR}/default.conf ${D}/var/lib/environment/compositor/
}
