require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Moto 360"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "minnow"

SRC_URI = " git://android.googlesource.com/kernel/omap;branch=android-omap-minnow-3.10-marshmallow-mr1-wear-release;protocol=https \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-dtcat-Port-to-Python-3.patch \
    file://0003-ARM-dma-mapping-fix-out-of-bounds-access-in-CMA.patch \
    file://0004-Fix-compilation-warnings.patch \
    file://0005-include-update-log2-header-from-the-Linux-kernel.patch \
    file://0006-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0007-random-introduce-getrandom-2-system-call.patch \
    file://0008-ARM-wire-up-getrandom-syscall.patch \
    file://0009-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://defconfig \
    file://img_info \
"

SRCREV = "f651c7734b8d1ca6f2e39764eda8f413831bcf81"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
