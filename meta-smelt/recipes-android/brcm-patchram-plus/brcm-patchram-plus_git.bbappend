FILESEXTRAPATHS_prepend_smelt := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_smelt = " file://patchram.service "
CFLAGS_append_smelt = " -DLPM_STURGEON"
