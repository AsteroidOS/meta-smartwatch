FILESEXTRAPATHS:prepend:ray := "${THISDIR}/sensorfw:"
SRC_URI:append:ray = " file://sensorfwd.service"

DEPENDS:append:ray = " libhybris "