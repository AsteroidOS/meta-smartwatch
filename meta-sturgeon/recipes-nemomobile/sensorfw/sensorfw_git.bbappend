FILESEXTRAPATHS_prepend_sturgeon := "${THISDIR}/sensorfw:"
SRC_URI_append_sturgeon = " file://sensorfwd.service \
                            file://0001-HybrisStepCounterAdapter-Set-delay-to-normal-speed.patch \
"

DEPENDS_append_sturgeon = " libhybris "
