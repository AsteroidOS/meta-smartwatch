FILESEXTRAPATHS:prepend:swift := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:swift = " file://patchram.service "
CFLAGS:append:swift = " -DLPM_SWIFT"
