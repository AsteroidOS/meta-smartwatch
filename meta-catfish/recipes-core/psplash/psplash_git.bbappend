FILESEXTRAPATHS:prepend:catfish := "${THISDIR}/${PN}:"

SRC_URI:append:catfish += " file://0001-Disable-double-buffering.patch"
SPLASH_IMAGES = "file://psplash-img-280.png;outsuffix=default"
