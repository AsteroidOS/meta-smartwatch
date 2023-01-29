FILESEXTRAPATHS:prepend:nemo := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:nemo = " file://patchram.service "
CFLAGS:append:nemo = " -DLPM_BASS"
