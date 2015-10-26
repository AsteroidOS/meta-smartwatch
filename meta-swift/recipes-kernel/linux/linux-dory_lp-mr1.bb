require recipes-kernel/linux/linux.inc
inherit gettext boot-img

SECTION = "kernel"
SUMMARY = "Android kernel for the LG G Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "dory"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-dory-3.10-lollipop-mr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info "
SRCREV = "9baeef88e425be653d8287f141ee209d78b918b3"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
S = "${WORKDIR}/git"
B = "${S}"

BOOT_PARTITION = "/dev/mmcblk0p15"

# Removes some headers that are installed incorrectly
do_install_append() {
    rm -rf ${D}/usr/src/usr/
}
