FILESEXTRAPATHS:prepend:medaka := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:medaka = "medaka"

RDEPENDS:${PN}:append:medaka = " msm-fb-refresher"

