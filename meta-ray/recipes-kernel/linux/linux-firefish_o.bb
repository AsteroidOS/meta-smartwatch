require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Skagen Falster 2"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "firefish"

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-firefish-3.18-oreo-wear-dr;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0003-touchscreen-raydium-Add-delay-for-wakeup-report.patch \
    file://0004-Backport-mainline-4.1-Bluetooth-subsystem.patch \
"

SRCREV = "20d62df1b6b88de89184cbd1bf826291f43ddec8"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+oreo"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p29"

inherit mkboot old-kernel-gcc-hdrs
