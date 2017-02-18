require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG G Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "swift"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-swift-3.10-lollipop-mr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0002-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch"
SRCREV = "9baeef88e425be653d8287f141ee209d78b918b3"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
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
