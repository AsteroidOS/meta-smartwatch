RDEPENDS:${PN}:append:harmony = " qt6-qpa-hwcomposer-plugin "
RDEPENDS:${PN}:append:inharmony = " qt6-qpa-hwcomposer-plugin "

FILESEXTRAPATHS:prepend:harmony := "${THISDIR}/lipstick:"
SRC_URI:append:harmony = " file://0001-Rotate-screen-for-harmony.patch"
