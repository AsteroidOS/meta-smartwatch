FILESEXTRAPATHS_prepend_tetra := "${THISDIR}/sensorfw:"
SRC_URI_append_tetra = " file://0001-HybrisAlsAdaptor-Set-default-delay.patch"
DEPENDS_append_tetra = " libhybris "
