require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the OPPO Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "beluga"

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-beluga-4.9-pie-wear-mr2;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0001-dts-Add-oppo-device-trees.patch \
    file://0001-cyttsp5-Fix-compilation.patch \
    file://0001-oppo-Fix-compilation.patch \
    file://0001-pinctrl-Fix-compilation.patch \
    file://0001-btfm_slim-Fix-compilation.patch \
    file://0001-cpuidle-Fix-compilation.patch \
    file://0001-gpu-Compilation-fix.patch \
    file://0001-vl53l0x-Fix-compilation.patch \
    file://0001-camera_v2-Fix-compilation.patch \
    file://0001-ipa_v2-Fix-compilation.patch \
    file://0001-usb-gadget-Fix-compilation.patch \
    file://0001-soc-qcom-Fix-compilation.patch \
    file://0001-Import-GPU-drivers-from-ray-oreo.patch \
"

SRCREV = "c9e8c2ecb9a8b9453504c12469b73a3947060a4e"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+oreo"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

BOOT_PARTITION = "/dev/mmcblk0p29"

inherit mkboot old-kernel-gcc-hdrs
