require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Ticwatch E & S"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "mooneye"

# Use an older version of gcc (gcc >= 9 doesn't boot.)
DEPENDS:remove = "virtual/${TARGET_PREFIX}gcc"
DEPENDS += "virtual/${TARGET_PREFIX}gcc8"

SRC_URI = "git://android.googlesource.com/kernel/mediatek;branch=android-mediatek-mooneye-4.4-oreo-wear-dr;protocol=https \
    file://0001-Fix-various-drivers-compilation-with-a-recent-GCC.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0004-Backport-HCI_STP-from-Mediatek.patch \
    file://0005-hci-don-t-set-MWS-command.patch \
    file://0006-hci_core-handle-incorrect-specified-max-page-number.patch \
    file://0007-platform_uart-Avoid-a-kernel-panic-when-restoring-an.patch \
    file://defconfig \
    file://img_info"
SRCREV = "faeb8c03bca6c09f8817f4d509e0280b53af8b99"
LINUX_VERSION ?= "4.4"
PV = "${LINUX_VERSION}+oreo"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p7"

inherit mkboot old-kernel-gcc-hdrs
