require recipes-kernel/linux/linux-yocto.inc
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
           file://0001-dts-Add-hoki-device-trees.patch \
           file://0002-mmc-Fix-embedded_sdio_data-duplicate-definition.patch \
           file://0003-video-fbdev-msm-Provide-mdss_dsi_switch_page.patch \
           file://0004-usb-hcd-Handle-when-host-mode-isn-t-available.patch \
           file://0005-initramfs-Don-t-skip-initramfs.patch \
           file://0006-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
           "

SRCREV = "c0b4c201f2d5a641defe19958a9b4c16f40d866b"
LINUX_VERSION ?= "4.14.206"
LINUX_VERSION_EXTENSION = ""
PE = "1"
PV = "${LINUX_VERSION}+git${SRCPV}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/

    # The ..install.cmd contains references to TMPDIR
    find ${D}/usr/src/ -name ..install.cmd | xargs rm -f
}

MKBOOTIMG_CMDLINE = "console=ttyMSM0,115200,n8 androidboot.console=ttyMSM0 androidboot.selinux=permissive androidboot.hardware=hoki user_debug=30 msm_rtb.filter=0x237 ehci-hcd.park=3 androidboot.bootdevice=7824900.sdhci lpm_levels.sleep_disabled=1 earlycon=msm_serial_dm,0x78b0000 vmalloc=300M androidboot.usbconfigfs=true loop.max_part=7 androidboot.memcg=true cgroup.memory=nokmem,nosocket buildvariant=user audit=0"
MKBOOTIMG_BOARD = "hoki"
MKBOOTIMG_ARGS = "--base 0x80000000 --kernel_offset 0x00008000 --ramdisk_offset 0x01000000 --tags_offset 0x0000100 --pagesize 4096"

inherit mkbootimg old-kernel-gcc-hdrs
