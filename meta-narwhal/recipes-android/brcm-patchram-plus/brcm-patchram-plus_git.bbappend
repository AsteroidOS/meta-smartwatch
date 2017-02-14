FILESEXTRAPATHS_prepend_sturgeon := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_sturgeon = " file://patchram.service "
CFLAGS_append_sturgeon = " -DLPM_STURGEON"
