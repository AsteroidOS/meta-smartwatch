FILESEXTRAPATHS_prepend_harmony := "${THISDIR}/geoclue-provider-hybris:"
SRC_URI_append_harmony = " file://0001-Adds-a-gnssStatusCallback-from-Mediatek-gps.h-compat.patch"

FILESEXTRAPATHS_prepend_inharmony := "${THISDIR}/geoclue-provider-hybris:"
SRC_URI_append_inharmony = " file://0001-Adds-a-gnssStatusCallback-from-Mediatek-gps.h-compat.patch"
