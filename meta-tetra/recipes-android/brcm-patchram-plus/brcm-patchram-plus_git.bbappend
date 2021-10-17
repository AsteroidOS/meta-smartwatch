FILESEXTRAPATHS:prepend:tetra := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:tetra = " file://patchram.service "
CFLAGS:append:tetra = " -DLPM_TETRA"
