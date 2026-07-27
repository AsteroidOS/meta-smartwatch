require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the OPPO Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "beluga"

# Use an older version of gcc (gcc >= 13 doesn't boot.)
inherit kernel-gcc8

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-beluga-4.9-pie-wear-mr2;protocol=https \
    file://defconfig \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-dts-Add-oppo-device-trees.patch \
    file://0003-cyttsp5-Fix-compilation.patch \
    file://0004-oppo-Fix-compilation.patch \
    file://0005-pinctrl-Fix-compilation.patch \
    file://0006-btfm_slim-Fix-compilation.patch \
    file://0007-cpuidle-Fix-compilation.patch \
    file://0008-gpu-Compilation-fix.patch \
    file://0009-vl53l0x-Fix-compilation.patch \
    file://0010-camera_v2-Fix-compilation.patch \
    file://0011-ipa_v2-Fix-compilation.patch \
    file://0012-usb-gadget-Fix-compilation.patch \
    file://0013-soc-qcom-Fix-compilation.patch \
    file://0014-initramfs-Don-t-skip-initramfs.patch \
    file://0015-touchscreen-cyttsp5-Add-delay-for-wakeup-report.patch \
    file://0016-dts-msm8909w-Enable-more-GPU-clock-frequencies.patch \
    file://0017-linux3.4-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
" 

SRCREV = "c9e8c2ecb9a8b9453504c12469b73a3947060a4e"
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

MKBOOTIMG_CMDLINE = "androidboot.hardware=beluga console=ttyHSL0,115200,n8 androidboot.selinux=permissive androidboot.console=ttyHSL0 msm_rtb.filter=0x237 ehci-hcd.park=3 lpm_levels.sleep_disabled=1 earlycon=msm_hsl_uart,0x78af000 buildvariant=user audit=0"
MKBOOTIMG_BOARD = "beluga"
MKBOOTIMG_ARGS = "--base 0x00000000 --kernel_offset 0x00008000 --ramdisk_offset 0x02000000 --tags_offset 0x01e00000 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
