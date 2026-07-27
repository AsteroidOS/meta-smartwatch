require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus ZenWatch 2"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sparrow"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-sparrow-3.10-marshmallow-mr1-wear-release;protocol=https \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-patch-psmouse-base-disable.patch \
    file://0003-static-inline-in-ARM-ftrace.h.patch \
    file://0004-Add-files-needed-for-device_trace.h.patch \
    file://0005-Create-copy-of-devfreq_trace.h.patch \
    file://0006-Patch-battery-values.patch \
    file://0007-it7260-Add-delay-for-wakeup-report.patch \
    file://0008-Remove-unecessary-include-to-axc_PM8226Charger.h.patch \
    file://0009-msm-mdss-mdp-Don-t-use-tracing-features.patch \
    file://0010-Makefile-patch-fixes-ASUS_SW_VER-error.patch \
    file://0011-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0012-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0013-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0014-Revert-Anthias-GPU-restrict-the-max-clock-rate-of-gp.patch \
    file://0015-random-introduce-getrandom-2-system-call.patch \
    file://0016-ARM-wire-up-getrandom-syscall.patch \
    file://0017-linux3.4-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0018-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
    file://defconfig "

SRCREV = "3d2fa521411917b5a1683fec42ad2a8e50aee79f"
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

MKBOOTIMG_CMDLINE = "console=ttyHSL0,115200,n8 androidboot.console=ttyHSL0 androidboot.hardware=sparrow user_debug=31 maxcpus=4 msm_rtb.filter=0x3F pm_levels.sleep_disabled=1 selinux=0 SYSTEMD_CGROUP_ENABLE_LEGACY_FORCE=1"
MKBOOTIMG_BOARD = "sparrow"
MKBOOTIMG_ARGS = "--base 0x00000000 --kernel_offset 0x00008000 --ramdisk_offset 0x02000000 --tags_offset 0x01e00000 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
