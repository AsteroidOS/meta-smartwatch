FILESEXTRAPATHS:prepend:nemo := "${THISDIR}/${PN}:"

SRC_URI:append:nemo = " \
    file://nemo-android-compat.conf \
    file://android-servicemanager.service \
"

do_install:append:nemo() {
    install -d ${D}/etc/tmpfiles.d
    install -m 0644 ${UNPACKDIR}/nemo-android-compat.conf ${D}/etc/tmpfiles.d/nemo-android-compat.conf

    install -m 0644 ${UNPACKDIR}/android-servicemanager.service ${D}${systemd_system_unitdir}/android-servicemanager.service
    install -d ${D}${systemd_system_unitdir}/multi-user.target.wants
    ln -s ../android-servicemanager.service ${D}${systemd_system_unitdir}/multi-user.target.wants/android-servicemanager.service
}

FILES:${PN}:append:nemo = " /etc/tmpfiles.d/nemo-android-compat.conf ${systemd_system_unitdir}/android-servicemanager.service ${systemd_system_unitdir}/multi-user.target.wants/android-servicemanager.service"
