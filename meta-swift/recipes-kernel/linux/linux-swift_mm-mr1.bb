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
    file://img_info \
    file://0002-firmware_class-Load-firmwares-from-Android-directori.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch"
SRCREV = "2f958570bcf7457da4827dc8da5ff3195d447cb3"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_configure_prepend() {
    sed -i "s/ASUS_SW_VER/\"aos1\"/" ${S}/kernel/asusevtlog.c
}

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/sda"

inherit mkboot old-kernel-gcc-hdrs
