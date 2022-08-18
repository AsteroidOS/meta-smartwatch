FILESEXTRAPATHS:prepend:sturgeon := "${THISDIR}/sensorfw:"
SRC_URI:append:sturgeon = " file://sensorfwd.service \
                            file://0001-HybrisStepCounterAdapter-Set-delay-to-normal-speed.patch \
                            file://0002-HybrisHrmAdaptor-Set-default-delay-to-200-ms.patch \
"
