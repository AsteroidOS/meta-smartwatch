require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Mainline kernel for the Asus ZenWatch 2"
HOMEPAGE = "https://kernel.org/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"
COMPATIBLE_MACHINE = "sparrow"

DEPENDS += "rsync-native"

SRC_URI = "git://github.com/z3ntu/linux;branch=qcom-msm8226-6.0.y-dsi;protocol=https \
    file://0001-ata-ahci-fix-enum-constants-for-gcc-13.patch \
    file://defconfig \
    file://img_info "
SRCREV = "70abf059e2b6984116e2e4fd3894fa91a829e248"
LINUX_VERSION ?= "6.0"
PV = "${LINUX_VERSION}"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

# The boot.img needs to embed in its "kernel" section the concatenation of the zImage with the device tree blob
KERNEL_OUTPUT = "${KERNEL_OUTPUT_DIR}/${KERNEL_IMAGETYPE}-dtb"
do_deploy:append() {
    cat ${KERNEL_OUTPUT_DIR}/${KERNEL_IMAGETYPE} ${KERNEL_OUTPUT_DIR}/dts/${KERNEL_DEVICETREE} > ${KERNEL_OUTPUT}
}

inherit mkboot
