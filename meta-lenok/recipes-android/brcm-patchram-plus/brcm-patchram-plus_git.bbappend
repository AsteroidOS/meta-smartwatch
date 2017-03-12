FILESEXTRAPATHS_prepend_lenok := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_lenok = " file://patchram.service "
CFLAGS_append_lenok = " -DLPM_LENOK"
