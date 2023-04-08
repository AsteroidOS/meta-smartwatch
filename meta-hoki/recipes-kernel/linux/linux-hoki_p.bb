require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Fossil Gen 6 platform"
HOMEPAGE = "https://github.com/fossil-engineering/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "hoki"

SRC_URI = " git://github.com/fossil-engineering/kernel-msm-fossil-cw;branch=fossil-android-msm-hoki-lw1.2-4.14;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-dts-Add-hoki-device-trees.patch \
" 

SRCREV = "c0b4c201f2d5a641defe19958a9b4c16f40d866b"
LINUX_VERSION ?= "4.14"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p29"

inherit mkboot old-kernel-gcc-hdrs
