FILESEXTRAPATHS:prepend:beluga := "${THISDIR}/sensorfw:"
SRC_URI:append:beluga = " file://sensorfwd.service"

DEPENDS:append:beluga = " libhybris "
