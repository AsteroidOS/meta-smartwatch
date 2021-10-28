FILESEXTRAPATHS_prepend_ray := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_ray = " file://patchram.service "
CFLAGS_append_ray = " -DLPM_SAWSHARK"
