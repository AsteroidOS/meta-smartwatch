FILESEXTRAPATHS:prepend:harmony := "${THISDIR}/geoclue-provider-hybris-hal:"
SRC_URI:append:harmony = " file://0001-Adds-a-gnssStatusCallback-from-Mediatek-gps.h-compat.patch"

FILESEXTRAPATHS:prepend:inharmony := "${THISDIR}/geoclue-provider-hybris-hal:"
SRC_URI:append:inharmony = " file://0001-Adds-a-gnssStatusCallback-from-Mediatek-gps.h-compat.patch"
