require recipes-kernel/linux/linux.inc
inherit gettext

SUMMARY = "Android kernel for the LG G Watch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"

SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-dory-3.10-lollipop-wear-release"
SRCREV = "${AUTOREV}"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"

COMPATIBLE_MACHINE = "dory"
S = "${WORKDIR}/git"
B = "${S}"

FILESEXTRAPATHS_prepend := "${THISDIR}/files-lp:"
SRC_URI += "file://defconfig"
