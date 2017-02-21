FILESEXTRAPATHS_prepend_swift := "${THISDIR}/brcm-patchram-plus:"
SRC_URI_append_swift = " file://patchram.service "
CFLAGS_append_swift = " -DLPM_SWIFT"
