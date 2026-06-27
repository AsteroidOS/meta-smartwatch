FILESEXTRAPATHS:prepend:nemo := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:nemo = " file://patchram.service file://BCM43430A1.hcd "
CFLAGS:append:nemo = " -DLPM_NEMO"
FILES:${PN}:append:nemo = " /vendor/firmware/BCM43430A1.hcd"

do_install:append:nemo() {
    install -d ${D}/vendor/firmware
    install -m 0644 ${UNPACKDIR}/BCM43430A1.hcd ${D}/vendor/firmware/BCM43430A1.hcd
}
