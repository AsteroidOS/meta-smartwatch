require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Huawei Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sturgeon"

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-sturgeon-3.10-marshmallow-mr1-wear-release-1;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-Revert-Enable-Nitrous-BT-power-management-driver.patch \
    file://0002-Revert-Add-Nitrous-driver-for-BT-power-management.patch \
    file://0003-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0004-fix-gcc5-build.patch \
    file://0005-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0006-tap-to-wake-fix.patch \
    file://0007-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0008-synaptics_i2c_rmi4-Adds-a-wakelock-when-the-screen-i.patch \
"

SRCREV = "9578569f9a10adcf75a14b8ed2da2e4921e7f8b4"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p15"

inherit mkboot old-kernel-gcc-hdrs
