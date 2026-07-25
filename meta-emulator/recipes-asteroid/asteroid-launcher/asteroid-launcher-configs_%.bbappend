FILESEXTRAPATHS:prepend:emulator := "${THISDIR}/asteroid-launcher-configs:"
SRC_URI:append:emulator = " file://kms.json"

do_install:append:emulator() {
        install -m 0644 ${UNPACKDIR}/kms.json ${D}/var/lib/environment/compositor/
}
