require recipes-kernel/linux/linux-yocto.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus Zenwatch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "anthias"

EXTRA_OEMAKE:append = " \
    KCFLAGS+=' -std=gnu17' \
    HOSTCFLAGS+=' -std=gnu17' \
"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-anthias-3.10-marshmallow-dr1-wear-release;protocol=https \
           file://defconfig \
           file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
           file://0002-Create-copy-of-devfreq_trace.h.patch \
           file://0003-msm-mdss-mdp-Don-t-use-tracing-features.patch \
           file://0004-psmouse-base-disable-references-to-lifebook_detect-w.patch \
           file://0006-traps-only-use-unwind_backtrace-if-available.patch \
           file://0007-Use-Bluesleep-instead-of-Nitrous-for-BT-power-manage.patch \
           file://0008-Backport-mainline-4.1-Bluetooth-subsystem.patch \
           file://0010-ARM-uaccess-remove-put_user-code-duplication.patch \
           file://0011-random-introduce-getrandom-2-system-call.patch \
           file://0012-ARM-wire-up-getrandom-syscall.patch \
           file://0013-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
           file://0014-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
           file://0015-Remove-spurious-line-from-Kconfig.patch \
           file://0009-it7260-Add-delay-for-wakeup-report.-AsteroidOS-requi.patch \
           file://0005-arm-LLVMLinux-use-static-inline-in-ARM-ftrace.h.patch \
           "
SRCREV = "5d054632429188226b8c1e1e545475c89ad4c582"
LINUX_VERSION ?= "3.10.40"
LINUX_VERSION_EXTENSION = ""
PV = "${LINUX_VERSION}+marshmallow"
B = "${S}"

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

    # The ..install.cmd contains references to TMPDIR
    find ${D}/usr/src/ -name ..install.cmd | xargs rm -f
}

MKBOOTIMG_CMDLINE = "console=ttyHSL0,115200,n8 androidboot.console=ttyHSL0 androidboot.hardware=anthias user_debug=31 maxcpus=4 msm_rtb.filter=0x3F androidboot.emmc=true androidboot.serialno=F9NZCY000803364 androidboot.bootloader=ANTHIAS1531 HW_ID=WI500Q_SR2  SB=Y SKU_ID=0 CPU_RV=108040e1 SBL_INFO=1015-0010-USR  quiet bootdbguart=y androidboot.baseband=apq mdss_mdp.panel=1:dsi:0:qcom,mdss_dsi_rm69032_320_cmd SYSTEMD_CGROUP_ENABLE_LEGACY_FORCE=1"
MKBOOTIMG_BOARD = "anthias"
MKBOOTIMG_ARGS = "--base 0x00000000 --kernel_offset 0x00008000 --ramdisk_offset 0x02000000 --tags_offset 0x01e00000 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
