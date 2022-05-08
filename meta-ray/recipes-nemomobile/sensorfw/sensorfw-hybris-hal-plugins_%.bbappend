FILESEXTRAPATHS:prepend:ray := "${THISDIR}/sensorfw:"
SRC_URI:append:ray = " file://sensorfwd.service"

FILESEXTRAPATHS:prepend:firefish := "${THISDIR}/sensorfw:"
SRC_URI:append:firefish = " file://sensorfwd.service"
