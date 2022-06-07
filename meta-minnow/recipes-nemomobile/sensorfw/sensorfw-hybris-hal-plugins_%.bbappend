FILESEXTRAPATHS:prepend:minnow := "${THISDIR}/sensorfw:"
SRC_URI:append:minnow = " file://sensorfwd.service \
                            file://0001-HybrisStepCounterAdapter-Set-delay-to-normal-speed.patch \
"
