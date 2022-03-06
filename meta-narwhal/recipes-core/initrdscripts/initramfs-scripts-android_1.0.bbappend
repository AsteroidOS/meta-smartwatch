FILESEXTRAPATHS:prepend:narwhal := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:narwhal = "narwhal"

RDEPENDS:${PN}:append:narwhal += "msm-fb-refresher"
