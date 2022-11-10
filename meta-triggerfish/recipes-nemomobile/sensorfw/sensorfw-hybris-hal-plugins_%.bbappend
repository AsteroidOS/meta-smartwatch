FILESEXTRAPATHS:prepend:triggerfish := "${THISDIR}/sensorfw:"
SRC_URI:append:triggerfish = " file://sensorfwd.service"
