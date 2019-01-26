require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus ZenWatch 2"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sparrow"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-sparrow-3.10-marshmallow-mr1-wear-release;protocol=https \
    file://0001-patch-psmouse-base-disable.patch \
    file://0002-static-inline-in-ARM-ftrace.h.patch \
    file://0003-Add-files-needed-for-device_trace.h.patch \
    file://0004-Create-copy-of-devfreq_trace.h.patch \
    file://0005-Patch-battery-values.patch \
    file://0006-Touch-screen-sleep-resume-patch.patch \
    file://0007-Remove-unecessary-include-to-axc_PM8226Charger.h.patch \
    file://0008-msm-mdss-mdp-Don-t-use-tracing-features.patch \
    file://0009-Makefile-patch-fixes-ASUS_SW_VER-error.patch \
    file://0010-firmware_class-Load-firmwares-from-Android-directori.patch \
    file://0011-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://defconfig \
    file://img_info "
SRCREV = "3d2fa521411917b5a1683fec42ad2a8e50aee79f"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p11"

inherit mkboot old-kernel-gcc-hdrs
