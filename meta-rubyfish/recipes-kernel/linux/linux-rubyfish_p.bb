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
    file://0001-Disable-tracing.patch \
    file://0002-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
    file://0003-initramfs-Don-t-skip-initramfs.patch \
    file://0004-dts-Add-stock-rubyfish-device-tree.patch \
    file://0005-dts-Add-stock-rover-device-tree.patch \
"

SRCREV = "c428ef3654d52e816308a6cf11009a1742f86c1c"
LINUX_VERSION ?= "4.9"
PV = "${LINUX_VERSION}+pie"
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
