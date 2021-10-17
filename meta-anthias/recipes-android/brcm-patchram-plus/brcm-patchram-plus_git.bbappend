FILESEXTRAPATHS:prepend:anthias := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:anthias = " file://patchram.service "
CFLAGS:append:anthias = " -DLPM_ANTHIAS"
