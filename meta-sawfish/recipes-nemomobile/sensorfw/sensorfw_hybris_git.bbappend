FILESEXTRAPATHS:prepend:sawfish := "${THISDIR}/sensorfw:"
SRC_URI:append:sawfish = " file://sensorfwd.service"
