require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG W7 (w315)"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "narwhal"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-narwhal-3.18-oreo-wear-dr;protocol=https \
           file://defconfig \
           file://img_info \
           file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
           file://0002-ARM-uaccess-remove-put_user-code-duplication.patch \
           file://0003-Backport-mainline-4.1-Bluetooth-subsystem.patch \
           file://0004-Backport-mainline-4.1-Bluetooth-drivers.patch \
           file://0005-video-mdp3-Continue-when-the-overlay-wasn-t-released.patch \
           file://0006-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
           "

SRCREV = "ef2880c313e30d5b99e138599fd3d81d90daae3e"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+oreo"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
