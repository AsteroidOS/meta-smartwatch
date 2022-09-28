FILESEXTRAPATHS:prepend:koi := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:koi = " file://patchram.service "
CFLAGS:append:koi = " -DLPM_SAWSHARK"
