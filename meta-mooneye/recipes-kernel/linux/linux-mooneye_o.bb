require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Ticwatch E & S"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "mooneye"

SRC_URI = "git://android.googlesource.com/kernel/mediatek;branch=android-mediatek-mooneye-3.18-nougat-mr1-wear;protocol=https \
    file://0001-Fix-various-drivers-compilation-with-a-recent-GCC.patch \
    file://0002-firmware_class-Load-firmwares-from-Android-directori.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://defconfig \
    file://img_info"
SRCREV = "9e5996b838eaf1a545efe8f7ad6e708f47e00e0c"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+oreo"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p7"

inherit mkboot old-kernel-gcc-hdrs
