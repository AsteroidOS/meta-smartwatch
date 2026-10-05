FILESEXTRAPATHS:prepend:catfish := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:catfish = "catfish"

RDEPENDS:${PN}:append:catfish = " msm-fb-refresher"
