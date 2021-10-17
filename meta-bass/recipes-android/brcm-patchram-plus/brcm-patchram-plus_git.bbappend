FILESEXTRAPATHS:prepend:bass := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:bass = " file://patchram.service "
CFLAGS:append:bass = " -DLPM_BASS"
