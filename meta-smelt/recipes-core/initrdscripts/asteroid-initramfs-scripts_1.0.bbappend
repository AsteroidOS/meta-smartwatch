FILESEXTRAPATHS:prepend:smelt := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:smelt = "smelt"
RDEPENDS:${PN}:remove:smelt = "android-tools-adbd"
