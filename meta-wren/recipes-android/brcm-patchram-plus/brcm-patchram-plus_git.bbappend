FILESEXTRAPATHS_prepend_wren := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_wren = " file://patchram.service "
CFLAGS_append_wren = " -DLPM_WREN"
