require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus ZenWatch 2"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "wren"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-wren-3.10-marshmallow-mr1-wear-release;protocol=https \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-patch-psmouse-base-disable.patch \
    file://0003-static-inline-in-ARM-ftrace.h.patch \
    file://0004-Add-files-needed-for-device_trace.h.patch \
    file://0005-Create-copy-of-devfreq_trace.h.patch \
    file://0006-Patch-battery-values.patch \
    file://0007-Remove-unecessary-include-to-axc_PM8226Charger.h.patch \
    file://0008-msm-mdss-mdp-Don-t-use-tracing-features.patch \
    file://0009-Makefile-patch-fixes-ASUS_SW_VER-error.patch \
    file://0010-Revert-sparrow-wren-bt-Remove-redundancy-code.patch \
    file://0011-Revert-Enable-Nitrous-BT-power-management-driver-for.patch \
    file://0012-Revert-Add-Nitrous-driver-for-BT-power-management.patch \
    file://0013-Patch-bluesleep.c.patch \
    file://0014-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0015-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0016-Revert-Sparrow-audio-security-patch-for-CVE-2016-206.patch \
    file://0017-Revert-Add-wake_peer-hook-to-MSM-HS-UART-driver.patch \
    file://0018-Use-generic-dump-function.patch \
    file://0019-Use-normal-touch-handling-all-the-time.patch \
    file://0020-Disable-isTouchLocked.patch \
    file://0021-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0022-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0023-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
    file://defconfig \
    file://img_info "
SRCREV = "00f21f748f01888888909f9f58280f5a363cd5f9"
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
