FILESEXTRAPATHS:prepend:dory := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:dory = " file://patchram.service "
CFLAGS:append:dory = " -DLPM_DORY"
