require recipes-kernel/linux/linux-yocto.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Samsung Gear Live"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sprat"

KCONFIG_MODE = "alldefconfig"

EXTRA_OEMAKE:append = " \
    KCFLAGS+=' -std=gnu17' \
    HOSTCFLAGS+=' -std=gnu17' \
"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-sprat-3.10-marshmallow-dr1-wear-release;protocol=https \
    file://defconfig \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0004-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0005-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
"
SRCREV = "e0702f61b2736fe749bc31aa06fbdc5349074c1a"
LINUX_VERSION ?= "3.10.40"
LINUX_VERSION_EXTENSION = ""
PE = "1"
PV = "${LINUX_VERSION}+git${SRCPV}"

# symbol_why.py cannot analyse this vendor tree: kconfiglib chokes on
# drivers/media/usb/stk1160/Kconfig:20 ("couldn't parse '.'").
do_kernel_configcheck() {
    :
}

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

MKBOOTIMG_CMDLINE = "console=ttyHSL0,115200,n8 androidboot.console=ttyHSL0 androidboot.hardware=sprat user_debug=31 maxcpus=4 msm_rtb.filter=0x3F androidboot.emmc=true androidboot.serialno=F9NZCY000803364 androidboot.bootloader=ANTHIAS1531 HW_ID=WI500Q_SR2  SB=Y SKU_ID=0 CPU_RV=108040e1 SBL_INFO=1015-0010-USR  quiet bootdbguart=y androidboot.baseband=apq mdss_mdp.panel=1:dsi:0:qcom,mdss_dsi_rm69032_320_cmd SYSTEMD_CGROUP_ENABLE_LEGACY_FORCE=1"
MKBOOTIMG_BOARD = "sprat"
MKBOOTIMG_ARGS = "--base 0x00000000 --kernel_offset 0x00008000 --ramdisk_offset 0x02000000 --tags_offset 0x01e00000 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
