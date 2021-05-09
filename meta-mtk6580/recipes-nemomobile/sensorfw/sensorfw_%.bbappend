FILESEXTRAPATHS_prepend_harmony := "${THISDIR}/sensorfw:"
FILESEXTRAPATHS_prepend_inharmony := "${THISDIR}/sensorfw:"

SRC_URI_append_harmony = " file://0001-HybrisWristGestureAdaptor-Use-tilt-sensor-as-tilt-to.patch"
SRC_URI_append_inharmony = " file://0001-HybrisWristGestureAdaptor-Use-tilt-sensor-as-tilt-to.patch"

DEPENDS_append_harmony = " libhybris "
DEPENDS_append_inharmony = " libhybris "
