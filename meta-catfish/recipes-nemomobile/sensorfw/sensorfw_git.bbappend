FILESEXTRAPATHS_prepend_sawshark := "${THISDIR}/sensorfw:"
SRC_URI_append_sawshark = " file://sensorfwd.service"

DEPENDS_append_sawshark = " libhybris "