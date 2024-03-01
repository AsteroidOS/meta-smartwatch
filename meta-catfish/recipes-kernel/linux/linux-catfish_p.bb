require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Ticwatch Pro"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "catfish"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-catshark-3.18-pie-wear-dr;protocol=https \
           file://defconfig \
           file://img_info \
           file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
           file://0002-usb-gadget-include-gadget-folder-to-fix-compilation.patch \
           file://0003-char-bbd2.0-Fix-include-error.patch \
           file://0004-Backport-mainline-4.1-Bluetooth-subsystem.patch \
           file://0005-Focaltech-Delay-when-sending-wakeup-event.patch \
           file://0006-dts-msm8909w-Enable-more-GPU-clock-frequencies.patch \
           file://0007-Add-missing-BT_6LOWPAN-option-to-bluetooth-Kconfig.patch \
           file://0008-Restore-bluetooth_6lowpan-to-Makefile.patch \
           file://0009-Move-const-to-the-right-location.patch \
           file://0010-include-linux-module.h-copy-__init-__exit-attrs-to-i.patch \
           file://0011-Compiler-Attributes-add-support-for-__copy-gcc-9.patch \
           file://0012-Fix-alignment-of-struct-members.patch \
           file://0013-Apply-many-small-fixes-for-6lowpan.patch \
           file://0014-catfish-export-symbols-needed-by-6lowpan.patch \
           "

SRCREV = "2b65638aaf038943506d1e6e7a942a4948490a42"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
