FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Replace the hybris precondition (a no-op on aurora: it gates on a
# /dev/.coldboot_done sentinel this container never produces) with one that
# waits for the container's display HAL to be binder-ready before the
# compositor starts. See aurora-android-hal-ready.service.
SRC_URI:append:aurora = " file://asteroid-launcher-precondition-aurora"

do_install:append:aurora() {
    install -m 0755 ${UNPACKDIR}/asteroid-launcher-precondition-aurora \
        ${D}/usr/bin/asteroid-launcher-precondition
}
