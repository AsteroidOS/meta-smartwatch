FILESEXTRAPATHS:prepend:sprat := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:sprat = " file://patchram.service "
CFLAGS:append:sprat = " -DLPM_SPRAT"
