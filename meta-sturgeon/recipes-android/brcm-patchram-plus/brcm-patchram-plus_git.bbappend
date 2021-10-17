FILESEXTRAPATHS:prepend:sturgeon := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:sturgeon = " file://patchram.service "
CFLAGS:append:sturgeon = " -DLPM_STURGEON"
