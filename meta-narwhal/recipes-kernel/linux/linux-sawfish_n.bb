require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Huawei Watch 2 Bluetooth"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sawfish"

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-sawshark-3.18-oreo-wear-dr;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0006-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0004-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0005-Revert-BT-Delete-the-file-board-8909-rfkill.c.patch \
    file://0006-bluetooth-Import-Bluesleep-driver.patch \
    file://0007-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0008-Bluetooth-Fix-incorrect-gpio-definition-in-device-tr.patch \
    file://0009-cyttp5-Add-delay-for-wakeup-report.patch \
"

SRCREV = "51d550c70b9e35dbeeaf93a505e94ecbef818291"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+nougat"
S = "${WORKDIR}/git"
B = "${S}"

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p29"

inherit mkboot old-kernel-gcc-hdrs
