FILESEXTRAPATHS_prepend_sawfish := "${THISDIR}/${PN}:"
SRC_URI_append_sawfish += " file://0001-Disable-double-buffering.patch"

SPLASH_IMAGES = "file://psplash-img-280.png;outsuffix=default"
