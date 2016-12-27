require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG G Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sturgeon"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-sturgeon-3.10-marshmallow-mr1-wear-release-1;protocol=https \
    file://defconfig \
    file://img_info \
    file://0002-fix-gcc5-build.patch \
"
#    file://0001-Backport-mainline-4.1-Bluetooth-subsystem.patch

SRCREV = "9578569f9a10adcf75a14b8ed2da2e4921e7f8b4"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

# Removes some headers that are installed incorrectly

do_configure_prepend() {
    # Fixes build with GCC5
    echo "#include <linux/compiler-gcc4.h>" > ${S}/include/linux/compiler-gcc5.h
    echo "#include <linux/compiler-gcc5.h>" > ${S}/include/linux/compiler-gcc6.h
}

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p15"

inherit mkboot
