FILESEXTRAPATHS:prepend:narwhal := "${THISDIR}/sensorfw:"
SRC_URI:append:narwhal = " file://sensorfwd.service"

DEPENDS:append:narwhal = " libhybris "
