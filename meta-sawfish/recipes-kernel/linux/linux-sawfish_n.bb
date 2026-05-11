require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Huawei Watch 2 Bluetooth"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sawfish"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-sawshark-3.18-nougat-mr1-wear;protocol=https \
           file://defconfig \
           file://img_info \
           file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
           file://0006-ARM-uaccess-remove-put_user-code-duplication.patch \
           file://0003-mdss-Import-video-driver-from-Marshmallow.patch \
           file://0004-Backport-mainline-4.1-Bluetooth-subsystem.patch \
           file://0005-Revert-BT-Delete-the-file-board-8909-rfkill.c.patch \
           file://0006-bluetooth-Import-Bluesleep-driver.patch \
           file://0007-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
           file://0008-Bluetooth-Fix-incorrect-gpio-definition-in-device-tr.patch \
           file://0009-cyttp5-Add-delay-for-wakeup-report.patch \
           file://0010-video-mdp3-Continue-when-the-overlay-wasn-t-released.patch \
           file://00011-dts-sawshark-Remove-USB-gadget-function-list.patch \
           file://0011-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
           "

SRCREV = "66cf3d5be07a599af417695e4f22f304af667979"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+nougat"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
