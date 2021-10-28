FILESEXTRAPATHS_prepend_ray := "${THISDIR}/sensorfw:"
SRC_URI_append_ray = " file://sensorfwd.service"

DEPENDS_append_ray = " libhybris "