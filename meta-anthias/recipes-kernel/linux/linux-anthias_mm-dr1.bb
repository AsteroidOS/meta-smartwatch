require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus Zenwatch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "anthias"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-anthias-3.10-marshmallow-dr1-wear-release;protocol=https \
           file://defconfig \
           file://img_info \
           file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
           file://0002-Create-copy-of-devfreq_trace.h.patch \
           file://0003-msm-mdss-mdp-Don-t-use-tracing-features.patch \
           file://0004-psmouse-base-disable-references-to-lifebook_detect-w.patch \
           file://0005-static-inline-in-ARM-ftrace.h.patch \
           file://0006-traps-only-use-unwind_backtrace-if-available.patch \
           file://0007-Use-Bluesleep-instead-of-Nitrous-for-BT-power-manage.patch \
           file://0008-Backport-mainline-4.1-Bluetooth-subsystem.patch \
           file://0009-it7260-Add-delay-for-wakeup-report.patch \
           file://0010-ARM-uaccess-remove-put_user-code-duplication.patch \
           file://0011-random-introduce-getrandom-2-system-call.patch \
           file://0012-ARM-wire-up-getrandom-syscall.patch \
           file://0013-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
           file://0014-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
           "
SRCREV = "5d054632429188226b8c1e1e545475c89ad4c582"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
