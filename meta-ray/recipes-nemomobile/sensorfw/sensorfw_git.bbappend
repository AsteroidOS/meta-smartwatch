FILESEXTRAPATHS_prepend_sturgeon := "${THISDIR}/sensorfw:"
SRC_URI_append_sturgeon = " file://sensorfwd.service"

DEPENDS_append_sturgeon = " libhybris "