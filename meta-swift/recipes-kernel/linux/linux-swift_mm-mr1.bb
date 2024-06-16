require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus Zenwatch 3"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "swift"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-swift-3.18-marshmallow-mr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0004-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    "
SRCREV = "2f958570bcf7457da4827dc8da5ff3195d447cb3"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_configure:prepend() {
    sed -i "s/ASUS_SW_VER/\"aos1\"/" ${S}/kernel/asusevtlog.c
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
