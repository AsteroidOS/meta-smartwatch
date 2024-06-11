inherit kernel
# Including this causes a super weird error:
# | /tmp/ccDg8okE.s:4834: Error: selected processor does not support requested special purpose register -- `mrs r2,cpsr'
#require recipes-kernel/linux/linux-yocto.inc

SECTION = "kernel"
SUMMARY = "linux-next with patches for Samsung smartwatches"
HOMEPAGE = "https://kernel.org"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"
COMPATIBLE_MACHINE = "rinato"

SRC_URI = " \
    git://git@github.com/casept/linux-samsung-smartwatch.git;protocol=https;branch=rinato \
    file://defconfig \
"
SRC_URI[sha256sum] = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

SRCREV = "cadac5d99c5a450481fa270bb9e6cf4e2f2134b5"

LINUX_VERSION ?= "next"
KERNEL_VERSION_SANITY_SKIP="1"

PV = "${LINUX_VERSION}"
S = "${WORKDIR}/git"
B = "${S}"

# S-Boot is too old for dtree support, have to use concatenated device tree
do_compile:append() {
        cat "${KERNEL_OUTPUT_DIR}/dts/${KERNEL_DEVICETREE}" >> "${KERNEL_OUTPUT_DIR}/${KERNEL_IMAGETYPE}"
}
