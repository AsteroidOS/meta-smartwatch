require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus ZenWatch 2"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "wren"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-wren-3.10-marshmallow-mr1-wear-release;protocol=https \
    file://0001-patch-psmouse-base-disable.patch \
    file://0001-static-inline-in-ARM-ftrace.h.patch \
    file://0001-Add-files-needed-for-device_trace.h.patch \
    file://0001-Create-copy-of-devfreq_trace.h.patch \
    file://0001-Patch-battery-values.patch \
    file://0001-Remove-unecessary-include-to-axc_PM8226Charger.h.patch \
    file://0001-msm-mdss-mdp-Don-t-use-tracing-features.patch \
    file://0001-Makefile-patch-fixes-ASUS_SW_VER-error.patch \
    file://0001-Revert-sparrow-wren-bt-Remove-redundancy-code.patch \
    file://0001-Revert-Enable-Nitrous-BT-power-management-driver-for.patch \
    file://0002-Revert-Add-Nitrous-driver-for-BT-power-management.patch \
    file://0001-Patch-bluesleep.c.patch \
    file://0001-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0002-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0001-Revert-Sparrow-audio-security-patch-for-CVE-2016-206.patch \
    file://0001-Revert-Add-wake_peer-hook-to-MSM-HS-UART-driver.patch \
    file://0001-Use-generic-dump-function.patch \
    file://0001-Use-normal-touch-handling-all-the-time.patch \
    file://0001-Disable-isTouchLocked.patch \
    file://defconfig \
    file://img_info "
SRCREV = "00f21f748f01888888909f9f58280f5a363cd5f9"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p11"

inherit mkboot old-kernel-gcc-hdrs
