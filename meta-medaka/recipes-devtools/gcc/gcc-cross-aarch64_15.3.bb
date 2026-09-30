TARGET_ARCH = "aarch64"
FILESEXTRAPATHS:prepend := "${THISDIR}/../../../../oe-core/meta/recipes-devtools/gcc/gcc/:"
require recipes-devtools/gcc/gcc-${PV}.inc
require recipes-devtools/gcc/gcc-cross.inc
