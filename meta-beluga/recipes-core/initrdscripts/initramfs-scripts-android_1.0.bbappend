FILESEXTRAPATHS:prepend:beluga := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:beluga = "beluga"

RDEPENDS:${PN}:append:beluga = " msm-fb-refresher"
