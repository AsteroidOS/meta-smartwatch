require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the TicWatch Pro 3 GPS platform"
HOMEPAGE = "https://github.com/mobvoi/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "rubyfish"

SRC_URI = " git://github.com/mobvoi/mobvoi-ticwatch-kernel;branch=mobvoi-android-msm-rover-4.9;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-Makefile-don-t-make-CC-be-a-python-wrapper.patch \
    file://0001-kgsl_events.c-fix-include.patch \
    file://0002-remove-tracepoints.patch \
    file://0003-remove-tracepoints.patch \
    file://0005-initramfs-Don-t-skip-initramfs.patch \
    file://0001-dts-add-rubyfish-usable.dts.patch \
    file://0001-dts-add-rover-usable-device-tree.patch \
    file://0001-don-t-build-the-broken-dtbs.patch \
    file://0001-pinconf-generic-work-around-buggy-device-tree.patch \
\
"

SRCREV = "c428ef3654d52e816308a6cf11009a1742f86c1c"
LINUX_VERSION ?= "4.9"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
