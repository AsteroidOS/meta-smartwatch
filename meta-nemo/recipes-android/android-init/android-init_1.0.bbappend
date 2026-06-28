FILESEXTRAPATHS:prepend:nemo := "${THISDIR}/${PN}:"

SRC_URI:append:nemo = " \
    file://nemo-android-compat.conf \
    file://property_contexts \
"

do_install:append:nemo() {
    install -d ${D}/etc/tmpfiles.d
    install -m 0644 ${UNPACKDIR}/nemo-android-compat.conf ${D}/etc/tmpfiles.d/nemo-android-compat.conf

    install -m 0644 ${UNPACKDIR}/property_contexts ${D}/property_contexts
}

FILES:${PN}:append:nemo = " /etc/tmpfiles.d/nemo-android-compat.conf /property_contexts"
