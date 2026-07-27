require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the TicWatch Pro 3 GPS platform"
HOMEPAGE = "https://github.com/mobvoi/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "rubyfish"

SRC_URI = " git://github.com/mobvoi/mobvoi-ticwatch-kernel;branch=mobvoi-android-msm-rover-4.9;protocol=https \
    file://defconfig \
    file://0001-Disable-tracing.patch \
    file://0002-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0003-initramfs-Don-t-skip-initramfs.patch \
    file://0004-dts-Add-stock-rubyfish-device-tree.patch \
    file://0005-dts-Add-stock-rover-device-tree.patch \
    file://0006-net-bcmhd-Allow-compilation-as-module.patch \
"

SRCREV = "c428ef3654d52e816308a6cf11009a1742f86c1c"
LINUX_VERSION ?= "4.9"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/

    # The ..install.cmd contains references to TMPDIR
    find ${D}/usr/src/ -name ..install.cmd | xargs rm -f
}

MKBOOTIMG_CMDLINE = "console=ttyMSM0,115200,n8 androidboot.console=ttyMSM0 androidboot.selinux=permissive androidboot.hardware=rubyfish user_debug=30 msm_rtb.filter=0x237 ehci-hcd.park=3 androidboot.bootdevice=7824900.sdhci lpm_levels.sleep_disabled=1 earlycon=msm_serial_dm,0x78b0000 vmalloc=300M androidboot.usbconfigfs=true loop.max_part=7 androidboot.memcg=true cgroup.memory=nokmem,nosocket buildvariant=user audit=0"
MKBOOTIMG_BOARD = "rubyfish"
MKBOOTIMG_ARGS = "--base 0x80000000 --kernel_offset 0x00008000 --ramdisk_offset 0x01000000 --tags_offset 0x0000100 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
