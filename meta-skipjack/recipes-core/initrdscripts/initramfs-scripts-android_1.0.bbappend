FILESEXTRAPATHS:prepend:skipjack := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:skipjack = "skipjack"

RDEPENDS:${PN}:append:skipjack += "msm-fb-refresher"
