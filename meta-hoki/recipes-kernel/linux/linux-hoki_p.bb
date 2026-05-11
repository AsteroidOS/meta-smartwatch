require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Fossil Gen 6 platform"
HOMEPAGE = "https://github.com/fossil-engineering/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "hoki"

# Use an older version of gcc (gcc >= 13 doesn't boot.)
inherit kernel-gcc8

SRC_URI = "git://github.com/fossil-engineering/kernel-msm-fossil-cw;branch=fossil-android-msm-hoki-lw1.2-4.14;protocol=https \
           file://defconfig \
           file://img_info \
           file://0001-dts-Add-hoki-device-trees.patch \
           file://0002-mmc-Fix-embedded_sdio_data-duplicate-definition.patch \
           file://0003-video-fbdev-msm-Provide-mdss_dsi_switch_page.patch \
           file://0004-usb-hcd-Handle-when-host-mode-isn-t-available.patch \
           file://0005-initramfs-Don-t-skip-initramfs.patch \
           file://0006-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
           "

SRCREV = "c0b4c201f2d5a641defe19958a9b4c16f40d866b"
LINUX_VERSION ?= "4.14"
PV = "${LINUX_VERSION}+pie"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/

    # The ..install.cmd contains references to TMPDIR
    find ${D}/usr/src/ -name ..install.cmd | xargs rm -f
}

inherit mkboot old-kernel-gcc-hdrs
