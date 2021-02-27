require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG G Watch R"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "lenok"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-lenok-3.10-marshmallow-mr1-wear-release;protocol=https \
           file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
	   file://0002-arm-LLVMLinux-use-static-inline-in-ARM-ftrace.h.patch \
           file://0003-Backport-mainline-4.1-Bluetooth-subsystem.patch \
           file://0004-Revert-nitrous-commits.patch \
           file://0005-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
           file://0006-include-update-log2-header-from-the-Linux-kernel.patch \
           file://0007-ARM-uaccess-remove-put_user-code-duplication.patch \
           file://defconfig \
           file://img_info \
           "
SRCREV = "2e918211eabb2843d87ac3c02baf5b03d84790f7"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p15"

inherit mkboot old-kernel-gcc-hdrs
