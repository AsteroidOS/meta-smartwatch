FILESEXTRAPATHS:prepend:smelt := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:smelt = " file://patchram.service "
CFLAGS:append:smelt = " -DLPM_SMELT"
