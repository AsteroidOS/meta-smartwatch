require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Sony Smartwatch 3"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "tetra"

SRC_URI = "git://android.googlesource.com/kernel/bcm;branch=android-bcm-tetra-3.10-marshmallow-dr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-Fix-build-with-OE.patch \
    file://0002-Bluetooth-fixes-a-poorly-done-patch.patch"

SRCREV = "0de8b342797a4074625055e77d37d5367d8ff285"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_configure_prepend() {
    # Fixes build with GCC5
    echo "#include <linux/compiler-gcc4.h>" > ${S}/include/linux/compiler-gcc5.h
    echo "#include <linux/compiler-gcc5.h>" > ${S}/include/linux/compiler-gcc6.h

    # This Kbuild is wanted by do_install
    mkdir -p ${S}/include/uapi/linux/broadcom
    touch ${S}/include/uapi/linux/broadcom/Kbuild
}

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p29"
MKBOOTIMG_ARGS = "--base 0x82000000"

inherit mkbootimg
