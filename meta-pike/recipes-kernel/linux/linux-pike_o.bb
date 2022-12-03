require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Polar M600"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "pike"

# Use an older version of gcc (gcc >= 9 doesn't boot.)
DEPENDS:remove = "virtual/${TARGET_PREFIX}gcc"
DEPENDS += "virtual/${TARGET_PREFIX}gcc8"

SRC_URI = "git://android.googlesource.com/kernel/mediatek;branch=android-mediatek-pike-3.10-oreo-wear-dr;protocol=https \
    file://0001-mediatek-ipanic-Remove-inline-to-fix-a-compilation-w.patch \
    file://0002-Fix-compilations-warnings.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://defconfig \
    file://img_info"
SRCREV = "5f7ba64dbb0f566149f5190db8c229da623a54bb"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+oreo"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p7"
inherit mkboot old-kernel-gcc-hdrs
