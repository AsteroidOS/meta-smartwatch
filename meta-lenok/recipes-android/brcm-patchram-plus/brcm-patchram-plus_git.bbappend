FILESEXTRAPATHS_prepend_dory := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_dory = " file://patchram.service "
CFLAGS_append_dory = " -DLPM_DORY"
