require recipes-kernel/linux/linux.inc
inherit gettext boot-img

SECTION = "kernel"
SUMMARY = "Android kernel for the LG Watch Urbane"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "bass"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-bass-3.10-lollipop-mr1-wear-release;protocol=https \
    file://0001-static-inline-in-ARM-ftrace.h.patch;striplevel=1 \
    file://defconfig \
    file://img_info "
SRCREV = "4bcdb1888f288bff5bed803dc79ee6a9121d71c7"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
S = "${WORKDIR}/git"
B = "${S}"

BOOT_PARTITION = "/dev/mmcblk0p15"

# Removes some headers that are installed incorrectly

do_configure_prepend() {
    # Fixes build with GCC5
    echo "#include <linux/compiler-gcc4.h>" > ${S}/include/linux/compiler-gcc5.h
}

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}
