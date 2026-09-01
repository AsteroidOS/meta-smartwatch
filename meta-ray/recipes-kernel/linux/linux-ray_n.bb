require recipes-kernel/linux/linux-yocto.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Skagen Falster 2"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "ray"

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-ray-3.18-oreo-wear-dr;protocol=https \
    file://defconfig \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0003-touchscreen-raydium-Add-delay-for-wakeup-report.patch \
    file://0004-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0005-video-mdp3-Continue-when-the-overlay-wasn-t-released.patch \
    file://0006-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
"

SRCREV = "ceb81fda35a733c904776eaaabd72dddf1e603c9"
LINUX_VERSION ?= "3.18.24"
LINUX_VERSION_EXTENSION = ""
PE = "1"
PV = "${LINUX_VERSION}+git${SRCPV}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

MKBOOTIMG_CMDLINE = "androidboot.hardware=ray console=ttyHSL0,115200,n8 androidboot.console=ttyHSL0 msm_rtb.filter=0x237 ehci-hcd.park=3 lpm_levels.sleep_disabled=1 earlycon=msm_hsl_uart,0x78af000 buildvariant=user selinux=0 SYSTEMD_CGROUP_ENABLE_LEGACY_FORCE=1"
MKBOOTIMG_BOARD = "ray"
MKBOOTIMG_ARGS = "--base 0x00000000 --kernel_offset 0x00008000 --ramdisk_offset 0x02000000 --tags_offset 0x01e00000 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
