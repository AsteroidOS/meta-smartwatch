FILESEXTRAPATHS:prepend:tetra := "${THISDIR}/sensorfw:"
SRC_URI:append:tetra = " file://0001-HybrisAlsAdaptor-Set-default-delay.patch"
DEPENDS:append:tetra = " libhybris "
