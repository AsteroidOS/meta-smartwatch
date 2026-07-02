TARGET_ARCH = "aarch64"
FILESEXTRAPATHS:prepend := "${THISDIR}/../../../../oe-core/meta/recipes-devtools/binutils/binutils/:"
require recipes-devtools/binutils/binutils-cross_${PV}.bb
