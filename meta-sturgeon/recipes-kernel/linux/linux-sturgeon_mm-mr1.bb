require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Huawei Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "sturgeon"

SRC_URI = " git://android.googlesource.com/kernel/msm;branch=android-msm-sturgeon-3.10-marshmallow-dr1-wear-release;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-Revert-Enable-Nitrous-BT-power-management-driver.patch \
    file://0003-Revert-Add-Nitrous-driver-for-BT-power-management.patch \
    file://0004-Revert-net-bluetooth-fix-CVE-2015-8956.patch \
    file://0005-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0006-fix-gcc5-build.patch \
    file://0007-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0008-tap-to-wake-fix.patch \
    file://0009-bluesleep-Use-kernel-s-HCI-events-instead-of-proc-bl.patch \
    file://0010-synaptics_i2c_rmi4-Adds-a-wakelock-when-the-screen-i.patch \
    file://0011-random-introduce-getrandom-2-system-call.patch \
    file://0012-ARM-wire-up-getrandom-syscall.patch \
    file://0013-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0014-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
"

SRCREV = "97abcf5b24684a46530ffc8a4748dd7ae6c2e65a"
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

inherit mkboot old-kernel-gcc-hdrs
