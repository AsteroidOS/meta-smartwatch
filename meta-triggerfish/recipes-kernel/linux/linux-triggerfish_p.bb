require recipes-kernel/linux/linux-yocto.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Fossil Gen 5 platform"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "triggerfish"

KCONFIG_MODE = "alldefconfig"

# Use an older version of gcc (gcc >= 9 doesn't boot.)
inherit kernel-gcc8

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-triggerfish-4.9-pie-wear-mr1;protocol=https \
           file://defconfig \
           file://0001-Add-Fossil-device-tree-files.patch \
           file://0002-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
           file://0003-Force-triggerfish-DTB-to-build.patch \
           file://0004-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
           file://0005-initramfs-Don-t-skip-initramfs.patch \
           "

SRCREV = "82824770e036a1e7576a299bc37b52d633d62675"
LINUX_VERSION ?= "4.9.112"
LINUX_VERSION_EXTENSION = ""
PE = "1"
PV = "${LINUX_VERSION}+git${SRCPV}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

MKBOOTIMG_CMDLINE = "androidboot.hardware=triggerfish console=ttyHSL0,115200,n8 androidboot.selinux=permissive androidboot.console=ttyHSL0 msm_rtb.filter=0x237 ehci-hcd.park=3 lpm_levels.sleep_disabled=1 earlycon=msm_hsl_uart,0x78af000 buildvariant=user audit=0"
MKBOOTIMG_BOARD = "triggerfish"
MKBOOTIMG_ARGS = "--base 0x00000000 --kernel_offset 0x00008000 --ramdisk_offset 0x02000000 --tags_offset 0x01e00000 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
