require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Ticwatch C2+"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "skipjack"

# Use an older version of gcc (gcc >= 9 doesn't boot.)
DEPENDS:remove += "virtual/${TARGET_PREFIX}gcc"
DEPENDS += "virtual/${TARGET_PREFIX}gcc8"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-skipjack-3.18-oreo-wear-dr;protocol=https \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-ARM-fix-put_user-for-gcc-8.patch \
    file://0003-touchscreen-Disable-extlibs.patch \
    file://0004-skipjack-ddr-add-dts-support-for-1GB-DDR.patch \
    file://0005-cgroups-Fix-compile-time-error.patch \
    file://0006-touchscreen-focaltech-Add-delay-for-wakeup-report.patch \
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

BOOT_PARTITION = "/dev/mmcblk0p37"

inherit mkboot old-kernel-gcc-hdrs
