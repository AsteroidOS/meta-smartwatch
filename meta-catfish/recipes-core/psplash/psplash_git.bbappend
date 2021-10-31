FILESEXTRAPATHS:prepend:ray := "${THISDIR}/${PN}:"

SRC_URI:append:ray += " file://0001-Disable-double-buffering.patch"
SPLASH_IMAGES = "file://psplash-img-280.png;outsuffix=default"
