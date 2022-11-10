FILESEXTRAPATHS:prepend:triggerfish := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:triggerfish = "triggerfish"

RDEPENDS:${PN}:append:triggerfish = " msm-fb-refresher"
