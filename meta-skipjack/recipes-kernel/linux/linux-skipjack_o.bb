require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Ticwatch C2+"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "skipjack"

# Use an older version of gcc (gcc >= 9 doesn't boot.)
DEPENDS_remove += "virtual/${TARGET_PREFIX}gcc"
DEPENDS += "virtual/${TARGET_PREFIX}gcc8"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-skipjack-3.18-pie-wear-dr;protocol=https \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-ARM-fix-put_user-for-gcc-8.patch \
    file://0003-touchscreen-Disable-extlibs.patch \
    file://defconfig \
    file://img_info"
SRCREV = "a3099f57d3d213992f2b123503b18d91975c7875"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p37"

inherit mkboot old-kernel-gcc-hdrs
