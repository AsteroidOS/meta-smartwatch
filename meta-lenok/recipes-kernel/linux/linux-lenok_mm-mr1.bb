require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG G Watch R"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
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
           file://0008-random-introduce-getrandom-2-system-call.patch \
           file://0009-ARM-wire-up-getrandom-syscall.patch \
           file://0010-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
           file://0011-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
           file://defconfig \
           file://img_info \
           "
SRCREV = "2e918211eabb2843d87ac3c02baf5b03d84790f7"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
