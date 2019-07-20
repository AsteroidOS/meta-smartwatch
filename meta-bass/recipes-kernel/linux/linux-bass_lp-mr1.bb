require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG Watch Urbane"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "bass"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-bass-3.10-lollipop-mr1-wear-release;protocol=https \
    file://0001-static-inline-in-ARM-ftrace.h.patch;striplevel=1 \
    file://0002-Backport-mainline-4.1-Bluetooth-subsystem.patch;striplevel=1 \
    file://0003-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0004-msm_pwm_vibrator-Convert-timed_output-APIs-to-ff_mem.patch \
    file://0006-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://defconfig \
    file://img_info "
SRCREV = "4bcdb1888f288bff5bed803dc79ee6a9121d71c7"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p15"

inherit mkboot old-kernel-gcc-hdrs
