FILESEXTRAPATHS:prepend:rinato := "${THISDIR}/asteroid-launcher-configs:"
SRC_URI:append:rinato = " file://rinato.conf file://kms-rinato.json"

do_install:append:rinato() {
        # Overwrite the default config.
        install -m 0644 ${WORKDIR}/rinato.conf ${D}/var/lib/environment/compositor/default.conf
        install -m 0644 ${WORKDIR}/kms-rinato.json ${D}/var/lib/environment/compositor/
}
