FILESEXTRAPATHS:prepend:minnow := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:minnow = " file://patchram.service "
CFLAGS:append:minnow = " -DLPM_STURGEON"
