FILESEXTRAPATHS:prepend:lenok := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:lenok = " file://patchram.service "
CFLAGS:append:lenok = " -DLPM_LENOK"
