FILESEXTRAPATHS_prepend_sawshark := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_sawshark = " file://patchram.service "
CFLAGS_append_sawshark = " -DLPM_SAWSHARK"
