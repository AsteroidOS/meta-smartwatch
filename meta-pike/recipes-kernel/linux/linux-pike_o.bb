require recipes-kernel/linux/linux-yocto.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Polar M600"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "pike"

# Use an older version of gcc (gcc >= 9 doesn't boot.)
inherit kernel-gcc8

# The 70 MB Linux backports (mainline 4.1 Bluetooth) patch is shared with harmony; reuse it instead of a copy.
FILESEXTRAPATHS:prepend := "${THISDIR}/../../../meta-mtk6580/recipes-kernel/linux:"

SRC_URI = "git://android.googlesource.com/kernel/mediatek;branch=android-mediatek-pike-3.10-oreo-wear-dr;protocol=https \
    file://0001-mediatek-ipanic-Remove-inline-to-fix-a-compilation-w.patch \
    file://0002-Fix-compilations-warnings.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0004-random-introduce-getrandom-2-system-call.patch \
    file://0005-ARM-wire-up-getrandom-syscall.patch \
    file://0006-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0007-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
    file://linux-harmony/0008-Backport-Bluetooth-subsytem-from-a-mailine-4.1-kerne.patch \
    file://0009-Backport-HCI_STP-for-MT6630-combo-Bluetooth.patch \
    file://0010-backports-drop-duplicate-bit_wait_io.patch \
    file://0011-backports-bt-skip-MWS-transport-config-on-MT6630.patch \
    file://0012-backports-Drop-the-trailing-slash-from-BACKPORT_DIR.patch \
    file://0013-backports-Anchor-the-include-paths-to-srctree.patch \
    file://defconfig \
    file://img_info"
SRCREV = "5f7ba64dbb0f566149f5190db8c229da623a54bb"
LINUX_VERSION ?= "3.10.57"
LINUX_VERSION_EXTENSION = ""
PE = "1"
PV = "${LINUX_VERSION}+git${SRCPV}"

# symbol_why.py cannot analyse this vendor tree: kconfiglib chokes on
# drivers/media/usb/stk1160/Kconfig:20 ("couldn't parse '.'").
do_kernel_configcheck() {
    :
}

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
