inherit kernel

SECTION = "kernel"
SUMMARY = "linux-next with patches for Samsung smartwatches"
HOMEPAGE = "https://kernel.org"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"
COMPATIBLE_MACHINE = "rinato"

SRC_URI = " git://git@github.com/casept/linux-samsung-smartwatch.git;protocol=https;branch=samsung-smartwatch "

SRCREV = "bf245d6426986d0b24e1d8d2a671726c2f994862"

LINUX_VERSION ?= "next"
KERNEL_VERSION_SANITY_SKIP = "1"

PV = "${LINUX_VERSION}"
B = "${S}"

# Just do it by hand, to be 1000% sure the right config is used here.
# Tried to do it with kernel-yocto.bbclass magic, ended up wasting 5 hours debugging.
do_configure() {
        make ARCH=arm rinato_debug_defconfig
}

# S-Boot is too old for dtree support, have to use concatenated device tree
do_compile:append() {
        cat "${KERNEL_OUTPUT_DIR}/dts/${KERNEL_DEVICETREE}" >> "${KERNEL_OUTPUT_DIR}/${KERNEL_IMAGETYPE}"
}
