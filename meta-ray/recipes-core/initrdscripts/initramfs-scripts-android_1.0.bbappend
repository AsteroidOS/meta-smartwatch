FILESEXTRAPATHS:prepend:ray := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:ray = "ray"

RDEPENDS:${PN}:append:ray += "msm-fb-refresher"

FILESEXTRAPATHS:prepend:firefish := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:firefish = "firefish"

RDEPENDS:${PN}:append:firefish += "msm-fb-refresher"
