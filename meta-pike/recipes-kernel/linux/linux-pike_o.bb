require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Polar M600"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "pike"

# Use an older version of gcc (gcc >= 9 doesn't boot.)
inherit kernel-gcc8

SRC_URI = "git://android.googlesource.com/kernel/mediatek;branch=android-mediatek-pike-3.10-oreo-wear-dr;protocol=https \
    file://0001-mediatek-ipanic-Remove-inline-to-fix-a-compilation-w.patch \
    file://0002-Fix-compilations-warnings.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0004-random-introduce-getrandom-2-system-call.patch \
    file://0005-ARM-wire-up-getrandom-syscall.patch \
    file://0006-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0007-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
    file://defconfig \
    file://img_info"
SRCREV = "5f7ba64dbb0f566149f5190db8c229da623a54bb"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+oreo"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
