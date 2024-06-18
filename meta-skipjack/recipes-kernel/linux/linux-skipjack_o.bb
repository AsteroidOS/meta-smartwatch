require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Ticwatch C2+"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "skipjack"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-skipjack-3.18-oreo-wear-dr;protocol=https \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-ARM-fix-put_user-for-gcc-8.patch \
    file://0003-touchscreen-Disable-extlibs.patch \
    file://0004-skipjack-ddr-add-dts-support-for-1GB-DDR.patch \
    file://0005-cgroups-Fix-compile-time-error.patch \
    file://0006-touchscreen-focaltech-Add-delay-for-wakeup-report.patch \
    file://0007-Backport-Bluetooth-4.1-subsystem.patch \
    file://0008-video-mdp3-Continue-when-the-overlay-wasn-t-released.patch \
    file://0009-touch-update-touch-firmware.patch \
    file://0010-touch-new-TP-ID-for-support-different-design.patch \
    file://0011-touch-new-TP-ID-for-support-different-design.patch \
    file://0012-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://defconfig \
    file://img_info"
SRCREV = "f46a8c36f416d4245b13b451c36f36a0c690283d"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
