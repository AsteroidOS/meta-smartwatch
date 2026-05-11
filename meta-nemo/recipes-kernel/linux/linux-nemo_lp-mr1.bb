require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the LG Watch Urbane 2nd edition"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "nemo"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-nemo-3.10-marshmallow-mr1-wear-release;protocol=https \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-static-inline-in-ARM-ftrace.h.patch;striplevel=1 \
    file://0003-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0004-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0005-vfs-allow-umount-to-handle-mountpoints-without-reval.patch \
    file://defconfig \
    file://img_info "
SRCREV = "504f3357f3ef296bf5ccbfe05df2025fa41eb354"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+marshmallow"
B = "${S}"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
