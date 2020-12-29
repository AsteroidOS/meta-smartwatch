FILESEXTRAPATHS_prepend_sawfish := "${THISDIR}/sensorfw:"
SRC_URI_append_sawfish = " file://sensorfwd.service"

DEPENDS_append_sawfish = " libhybris "