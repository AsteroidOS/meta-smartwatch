require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Casio F20"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "koi"
SRC_URI = " git://android.googlesource.com/kernel/exynos;branch=android-exynos-koi-3.18-oreo-dr-wear;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-ARM-uaccess-remove-put_user-code-duplication.patch \
    file://0003-Fix-compilations-warnings.patch \
    file://0004-Backport-mainline-4.1-Bluetooth-subsystem.patch \
    file://0005-Backport-mainline-4.1-Bluetooth-drivers.patch \
    file://0006-ARM-8933-1-replace-Sun-Solaris-style-flag-on-section.patch \
"

SRCREV = "f684256405854c40b5ccc2d126f810cf4c29ca2f"
LINUX_VERSION ?= "3.18"
PV = "${LINUX_VERSION}+oreo"
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
