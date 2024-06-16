require recipes-kernel/linux/linux.inc

SECTION = "kernel"
SUMMARY = "Android kernel for harmony"
HOMEPAGE = "https://github.com/OpenWatchProject/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "harmony|inharmony"

SRC_URI = "git://github.com/OpenWatchProject/android_kernel_mediatek_mt6580;protocol=https;branch=android-8.1 \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-printk-Expose-mt_enable_uart-and-mt_disable_uart-for.patch \
    file://0003-focaltech-Don-t-include-a-missing-firmware.patch \
    file://0004-Removes-some-references-to-the-low-memory-killer.patch \
    file://0005-Reduce-kernel-verbosity.patch \
    file://0006-Removes-some-Android-build-process-files-that-should.patch \
    file://0007-mtk_vibrator-Convert-timed_output-APIs-to-ff_memless.patch \
    file://0008-Backport-Bluetooth-subsytem-from-a-mailine-4.1-kerne.patch \
    file://0009-Backport-HCI_STP-from-Mediatek-with-some-patches-fro.patch \
    file://0010-IT7260-Don-t-suspend-the-controller-while-the-watch-.patch \
    file://0011-sec_dev-Fix-firmware-loading.patch \
    file://0012-Disable-new-gcc-7.1.1-warnings.patch \
    file://0013-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0014-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    "
SRC_URI:append:inharmony = "file://inharmonyconfig"

SRCREV = "b1ebbe66774b96f03fb440860d328a119b7f9a6b"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
S = "${WORKDIR}/git"
B = "${S}"

# Removes some headers that are installed incorrectly

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
