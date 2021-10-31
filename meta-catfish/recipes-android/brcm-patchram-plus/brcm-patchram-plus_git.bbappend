FILESEXTRAPATHS:prepend:ray := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:ray = " file://patchram.service "
CFLAGS:append:ray = " -DLPM_SAWSHARK"
