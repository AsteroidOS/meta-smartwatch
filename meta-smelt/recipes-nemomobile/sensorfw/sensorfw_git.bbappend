FILESEXTRAPATHS_prepend_smelt := "${THISDIR}/sensorfw:"
SRC_URI_append_smelt = " file://sensorfwd.service"

DEPENDS_append_smelt = " libhybris "