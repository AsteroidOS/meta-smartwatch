FILESEXTRAPATHS_prepend_sawfish := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_sawfish = " file://patchram.service "
CFLAGS_append_sawfish = " -DLPM_SAWSHARK"
