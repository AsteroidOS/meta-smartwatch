require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Casio WSD-F21hr"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "medaka"

SRC_URI = " git://android.googlesource.com/kernel/exynos;branch=android-exynos-medaka-4.4-pie-wear-dr;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
    file://0002-dw_mmc-condition-on-CONFIG_BCMDHD_SDIO-instead-of-CO.patch \
    file://0003-bcmdhd-always-load-firmware-from-BCMDHD_FW_PATH.patch \
" 

SRCREV = "6115e9fa122a3d1d38af5696b34fdd2c7364d7f9"
LINUX_VERSION ?= "4.4"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

# The kernel on this device is 64 bit, but the android libs are 32 bit, so we basically need to build all of userspace as 32 bit. 
# So we use a series of hacks to build just this kernel package as aarch64. This includes the below overrides, as well as weird gcc&bintools packages which provide aarch64 tooling for an otherwise arm(32) target
TUNE_ARCH = "aarch64"
# thanks argosphil for figuring out this skip
INSANE_SKIP += "arch"

KERNEL_IMAGETYPE = "Image.gz-dtb"

EXTRA_OEMAKE:append = " KCFLAGS='-Wno-error -Wno-error=deprecated-declarations' HOSTCFLAGS=-Wno-error"

do_configure:prepend() {
    install -m 644 -D ${UNPACKDIR}/defconfig ${WORKDIR}/defconfig
}


do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
