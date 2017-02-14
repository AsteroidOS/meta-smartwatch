FILESEXTRAPATHS_prepend_tetra := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_tetra = " file://patchram.service "
CFLAGS_append_tetra = " -DLPM_TETRA"
