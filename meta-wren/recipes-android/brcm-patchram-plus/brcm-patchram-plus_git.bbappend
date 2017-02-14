FILESEXTRAPATHS_prepend_sparrow := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_sparrow = " file://patchram.service "
CFLAGS_append_sparrow = " -DLPM_SPARROW"
