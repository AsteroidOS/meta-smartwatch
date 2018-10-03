RDEPENDS_${PN}_append_harmony = " qt5-qpa-hwcomposer-plugin "
RDEPENDS_${PN}_append_inharmony = " qt5-qpa-hwcomposer-plugin "

FILESEXTRAPATHS_prepend_harmony := "${THISDIR}/lipstick:"
SRC_URI_append_harmony = " file://0001-Rotate-screen-for-harmony.patch"
