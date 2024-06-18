require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Sony Smartwatch 3"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "tetra"

# Use an older version of gcc (gcc >= 9 doesn't boot.)
DEPENDS:remove = "virtual/${TARGET_PREFIX}gcc"
DEPENDS += "virtual/${TARGET_PREFIX}gcc8"

SRC_URI = "git://android.googlesource.com/kernel/bcm;branch=android-bcm-tetra-3.10-marshmallow-dr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-Fix-build-with-OE.patch \
    file://0003-Bluetooth-fixes-a-poorly-done-patch.patch \
    file://0004-ion-don-t-use-lmk.patch \
    file://0005-bcm_bzhw-Use-inverted-IRQ-lines-polarity.patch \
    file://0006-java_sony_brooks_020-Use-the-bcm-bzhw-driver-instead.patch \
    file://0007-Backport-v4.1-drivers-using-.-gentree.py-integrate-c.patch \
    file://0008-Integrate-hci_brcm-and-bcm_bzhw-to-the-backported-BT.patch \
    file://0009-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0010-give-up-on-gcc-ilog2-constant-optimizations.patch \
    file://0011-broadcom-modem-rpc-Disable-logs-which-cause-a-compil.patch \
    file://0012-random-introduce-getrandom-2-system-call.patch \
    file://0013-ARM-wire-up-getrandom-syscall.patch \
    file://0014-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
"
SRCREV = "0de8b342797a4074625055e77d37d5367d8ff285"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_configure:prepend() {
    # This Kbuild is wanted by do_install
    mkdir -p ${S}/include/uapi/linux/broadcom
    touch ${S}/include/uapi/linux/broadcom/Kbuild
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

MKBOOTIMG_ARGS = "--base 0x82000000"

inherit mkbootimg old-kernel-gcc-hdrs
