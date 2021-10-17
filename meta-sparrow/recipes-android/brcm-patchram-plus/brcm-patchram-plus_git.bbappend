FILESEXTRAPATHS:prepend:sparrow := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:sparrow = " file://patchram.service "
CFLAGS:append:sparrow = " -DLPM_SPARROW"
