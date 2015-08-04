require recipes-kernel/linux/linux.inc
inherit gettext kernel_android

SECTION = "kernel"
SUMMARY = "Android kernel for the LG G Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "dory"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-dory-3.10-lollipop-mr1-wear-release;protocol=https \
    file://defconfig"
SRCREV = "${AUTOREV}"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
S = "${WORKDIR}/git"
B = "${S}"

CMDLINE = "androidboot.hardware=dory user_debug=31 maxcpus=4 msm_rtb.filter=0x3F enable_adb ramoops.mem_address=0x30000000 ramoops.mem_size=0x200000 selinux=0"
KERNEL_RAM_BASE = "0x8000"
RAMDISK_RAM_BASE = "0x2000000"
SECOND_RAM_BASE = "0xf00000"
TAGS_RAM_BASE = "0x1e00000"
BOOT_PARTITION = "/dev/mmcblk0p15"

# Removes some headers that are installed incorrectly
do_install_append() {
    rm -rf ${D}/usr/src/usr/
}
