require recipes-kernel/linux/linux.inc
inherit gettext

SECTION = "kernel"
SUMMARY = "Android kernel for the Casio WSD-F21hr"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "medaka"

#    file://0016-dts-msm8909w-Enable-more-GPU-clock-frequencies.patch 
SRC_URI = " git://android.googlesource.com/kernel/exynos;branch=android-exynos-medaka-4.4-pie-wear-dr;protocol=https \
    file://defconfig \
    file://img_info \
    file://0001-scripts-dtc-Remove-redundant-YYLOC-global-declaratio.patch \
" 

SRCREV = "6115e9fa122a3d1d38af5696b34fdd2c7364d7f9"
LINUX_VERSION ?= "4.4"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

do_install:append() {
    rm -rf ${D}/usr/src/usr/
}

inherit mkboot old-kernel-gcc-hdrs
