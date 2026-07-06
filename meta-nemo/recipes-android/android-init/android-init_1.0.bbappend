FILESEXTRAPATHS:prepend:nemo := "${THISDIR}/${PN}:"

SRC_URI:append:nemo = " \
    file://nemo-android-compat.conf \
    file://nemo-bdaddr.conf \
    file://property_contexts \
"

do_install:append:nemo() {
    install -d ${D}/etc/tmpfiles.d
    install -m 0644 ${UNPACKDIR}/nemo-android-compat.conf ${D}/etc/tmpfiles.d/nemo-android-compat.conf
    install -m 0644 ${UNPACKDIR}/nemo-bdaddr.conf ${D}/etc/tmpfiles.d/nemo-bdaddr.conf

    install -m 0644 ${UNPACKDIR}/property_contexts ${D}/property_contexts
}

FILES:${PN}:append:nemo = " /etc/tmpfiles.d/nemo-android-compat.conf /etc/tmpfiles.d/nemo-bdaddr.conf /property_contexts"
