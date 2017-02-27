FILESEXTRAPATHS_prepend_sprat := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_sprat = " file://patchram.service "
CFLAGS_append_sprat = " -DLPM_SPRAT"
