FILESEXTRAPATHS:prepend:harmony := "${THISDIR}/sensorfw:"
FILESEXTRAPATHS:prepend:inharmony := "${THISDIR}/sensorfw:"

SRC_URI:append:harmony = " file://0001-HybrisWristGestureAdaptor-Use-tilt-sensor-as-tilt-to.patch"
SRC_URI:append:inharmony = " file://0001-HybrisWristGestureAdaptor-Use-tilt-sensor-as-tilt-to.patch"
