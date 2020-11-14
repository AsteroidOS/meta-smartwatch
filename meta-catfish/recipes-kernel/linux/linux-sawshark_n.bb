require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Huawei Watch 2"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sawshark"

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-sawshark-3.18-nougat-mr1-wear-release-1;protocol=https \
    file://defconfig \
    file://img_info \
    file://0006-ARM-uaccess-remove-put_user-code-duplication.patch \
"

SRCREV = "6c03f79a1f550065cc27ac6afed1e27e103d3935"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+nougat"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p29"

inherit mkboot old-kernel-gcc-hdrs
