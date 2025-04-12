require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG Watch Urbane"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "bass"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-bass-3.10-lollipop-mr1-wear-release;protocol=https \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-static-inline-in-ARM-ftrace.h.patch;striplevel=1 \
    file://0003-Backport-mainline-4.1-Bluetooth-subsystem.patch;striplevel=1 \
    file://0004-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0005-msm_pwm_vibrator-Convert-timed_output-APIs-to-ff_mem.patch \
    file://0006-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0007-random-introduce-getrandom-2-system-call.patch \
    file://0008-ARM-wire-up-getrandom-syscall.patch \
    file://0009-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0010-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
    file://defconfig \
    file://img_info "
SRCREV = "4bcdb1888f288bff5bed803dc79ee6a9121d71c7"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
S = "${WORKDIR}/git"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
