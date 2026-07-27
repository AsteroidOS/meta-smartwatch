require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus Zenwatch 3"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "swift"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-swift-3.18-marshmallow-mr1-wear-release;protocol=https \
    file://defconfig \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0004-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    "
SRCREV = "2f958570bcf7457da4827dc8da5ff3195d447cb3"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig

    sed -i "s/ASUS_SW_VER/\"aos1\"/" ${S}/kernel/asusevtlog.c
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

MKBOOTIMG_CMDLINE = "sched_enable_hmp=0 console=ttyHSL0,115200,n8 androidboot.console=ttyHSL0 androidboot.hardware=swift msm_rtb.filter=0x237 ehci-hcd.park=3 lpm_levels.sleep_disabled=1 earlyprintk androidboot.bootdevice=7824900.sdhci androidboot.serialno=GANZCY00151841D androidboot.bootloader=swift1003307 HW_ID=SWIFT_PR  CPU_RV=000520e1 androidboot.bootreason=reboot quiet androidboot.rebootmode=0x77665501 oem_perf_on androidboot.baseband=apq mdss_mdp3.panel=1:dsi:0:qcom,mdss_dsi_h139bln01_400_cmd:1:none SYSTEMD_CGROUP_ENABLE_LEGACY_FORCE=1"
MKBOOTIMG_BOARD = "swift"
MKBOOTIMG_ARGS = "--base 0x80000000 --kernel_offset 0x00008000 --ramdisk_offset 0x01000000 --tags_offset 0x00000100 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
