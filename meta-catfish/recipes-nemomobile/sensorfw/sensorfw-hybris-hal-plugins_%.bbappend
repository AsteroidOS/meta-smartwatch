FILESEXTRAPATHS:prepend:catfish := "${THISDIR}/sensorfw:"
SRC_URI:append:catfish = " file://sensorfwd.service"
