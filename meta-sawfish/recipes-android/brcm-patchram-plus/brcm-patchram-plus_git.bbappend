FILESEXTRAPATHS:prepend:sawfish := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:sawfish = " file://patchram.service "
CFLAGS:append:sawfish = " -DLPM_SAWSHARK"
