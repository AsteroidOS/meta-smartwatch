require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Moto 360 2015"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "smelt"

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-smelt-3.10-marshmallow-mr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-Revert-Enable-Nitrous-BT-power-management-driver.patch \
    file://0003-Revert-Add-Nitrous-driver-for-BT-power-management.patch \
    file://0004-fix-input-undefined.patch \
    file://0005-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0006-fix-gcc5-build.patch \
    file://0007-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0008-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0009-mdss_mdp_pipe-Don-t-allocate-first-RGB-pipe.patch \
    file://0010-random-introduce-getrandom-2-system-call.patch \
    file://0011-ARM-wire-up-getrandom-syscall.patch \
    file://0012-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
"

SRCREV = "49608c8bfc75360f7ac54f539ce326b90034bc9d"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
