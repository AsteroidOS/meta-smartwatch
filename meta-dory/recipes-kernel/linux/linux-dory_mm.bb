require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG G Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "dory"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-dory-3.10-marshmallow-mr1-wear-release;protocol=https \
    file://defconfig \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-Revert-Enable-Nitrous-BT-power-management-driver-for.patch \
    file://0003-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0004-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0005-msm_pwm_vibrator-Convert-timed_output-APIs-to-ff_mem.patch \
    file://0006-synaptics_i2c_rmi4-Adds-a-wakelock-when-the-screen-i.patch \
    file://0007-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0008-random-introduce-getrandom-2-system-call.patch \
    file://0009-ARM-wire-up-getrandom-syscall.patch \
    file://0010-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0011-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
"
SRCREV = "6924014484d3406e3d2da384efc20e40e8a5ae80"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
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

MKBOOTIMG_CMDLINE = "androidboot.hardware=dory user_debug=31 maxcpus=4 msm_rtb.filter=0x3F pm_levels.sleep_disabled=1 selinux=0 SYSTEMD_CGROUP_ENABLE_LEGACY_FORCE=1"
MKBOOTIMG_BOARD = "dory"
MKBOOTIMG_ARGS = "--base 0x00000000 --kernel_offset 0x00008000 --ramdisk_offset 0x02000000 --tags_offset 0x01e00000 --pagesize 2048"

inherit mkbootimg old-kernel-gcc-hdrs
