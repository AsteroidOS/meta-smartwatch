require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Sony Smartwatch 3"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "tetra"

SRC_URI = "git://android.googlesource.com/kernel/bcm;branch=android-bcm-tetra-3.10-lollipop-mr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-Fix-build-with-OE.patch \
    file://0002-Bluetooth-fixes-a-poorly-done-patch.patch \
    file://0003-ion-don-t-use-lmk.patch \
    file://0004-bcm_bzhw-Use-inverted-IRQ-lines-polarity.patch \
	file://0005-java_sony_brooks_020-Use-the-bcm-bzhw-driver-instead.patch \
    file://0006-Backport-v4.1-drivers-using-.-gentree.py-integrate-c.patch \
    file://0007-Integrate-hci_brcm-and-bcm_bzhw-to-the-backported-BT.patch \
    file://0008-firmware_class-Load-firmwares-from-Android-directori.patch \
    file://0009-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0010-give-up-on-gcc-ilog2-constant-optimizations.patch \
    file://0011-broadcom-modem-rpc-Disable-logs-which-cause-a-compil.patch"
SRCREV = "34ff7fc1fe7a771816ee65eb38fb0f74dd3feda7"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
S = "${WORKDIR}/git"
B = "${S}"

do_configure_prepend() {
    # This Kbuild is wanted by do_install
    mkdir -p ${S}/include/uapi/linux/broadcom
    touch ${S}/include/uapi/linux/broadcom/Kbuild
}

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p29"
MKBOOTIMG_ARGS = "--base 0x82000000"

inherit mkbootimg old-kernel-gcc-hdrs
