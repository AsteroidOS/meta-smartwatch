FILESEXTRAPATHS:prepend:wren := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:wren = " file://patchram.service "
CFLAGS:append:wren = " -DLPM_WREN"
