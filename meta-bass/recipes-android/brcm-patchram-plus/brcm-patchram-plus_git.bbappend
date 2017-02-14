FILESEXTRAPATHS_prepend_bass := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_bass = " file://patchram.service "
CFLAGS_append_bass = " -DLPM_BASS"
