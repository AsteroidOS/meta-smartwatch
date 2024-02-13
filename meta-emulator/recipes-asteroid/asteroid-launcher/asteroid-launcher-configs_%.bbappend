FILESEXTRAPATHS:prepend:emulator := "${THISDIR}/asteroid-launcher-configs:"
SRC_URI:append:emulator = " file://kms-qemu.json "

do_install:append:emulator() {
    install -m 0644 ${WORKDIR}/kms-qemu.json ${D}/var/lib/environment/compositor/
}
