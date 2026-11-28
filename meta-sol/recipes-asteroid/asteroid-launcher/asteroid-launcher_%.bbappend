FILESEXTRAPATHS:prepend:sol := "${THISDIR}/files:"

# sol's compositor has to wait for a hwcomposer that lives inside the Android
# container, where no systemd unit can reach it. Shipped as a drop-in plus a
# helper rather than as a change to the upstream unit, so nothing outside
# meta-sol is touched and every other device is unaffected.
SRC_URI:append:sol = " file://sol-wait-composer.sh \
                       file://50-sol-wait-for-composer.conf"

do_install:append:sol() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-wait-composer.sh ${D}${libexecdir}/sol-wait-composer.sh

    install -d ${D}${systemd_user_unitdir}/asteroid-launcher.service.d
    install -m 0644 ${UNPACKDIR}/50-sol-wait-for-composer.conf \
        ${D}${systemd_user_unitdir}/asteroid-launcher.service.d/50-sol-wait-for-composer.conf
}

FILES:${PN}:append:sol = " ${libexecdir}/sol-wait-composer.sh \
                           ${systemd_user_unitdir}/asteroid-launcher.service.d"
