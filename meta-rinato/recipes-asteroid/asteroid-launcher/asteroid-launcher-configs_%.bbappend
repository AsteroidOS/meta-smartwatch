FILESEXTRAPATHS:prepend:rinato := "${THISDIR}/asteroid-launcher-configs:"
SRC_URI:append:rinato = " file://kms-rinato.json "

do_install:append:rinato() {
        install -m 0644 ${WORKDIR}/kms-rinato.json ${D}/var/lib/environment/compositor/
}
