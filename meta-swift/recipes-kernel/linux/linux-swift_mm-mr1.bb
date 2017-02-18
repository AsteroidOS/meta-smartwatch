require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus Zenwatch 3"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "swift"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-swift-3.18-marshmallow-mr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info"
SRCREV = "2f958570bcf7457da4827dc8da5ff3195d447cb3"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

# Removes some headers that are installed incorrectly

do_configure_prepend() {
    # Fixes build with GCC5
    echo "#include <linux/compiler-gcc4.h>" > ${S}/include/linux/compiler-gcc5.h
    echo "#include <linux/compiler-gcc5.h>" > ${S}/include/linux/compiler-gcc6.h

    sed -i "s/ASUS_SW_VER/\"aos1\"/" ${S}/kernel/asusevtlog.c
}

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/sda"

inherit mkboot
