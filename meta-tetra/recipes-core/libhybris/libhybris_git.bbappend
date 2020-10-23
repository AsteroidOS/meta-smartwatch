FILESEXTRAPATHS_prepend_tetra := "${THISDIR}/libhybris:"
SRCREV_tetra = "4aa3379de1d911f00a86cb5643f7ff63dd48d7a6"
SRC_URI_append_tetra = " file://0001-Disable-wifi-module.patch;striplevel=2"

EXTRA_OECONF_append_tetra = " --enable-experimental"