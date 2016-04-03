require recipes-kernel/linux/linux.inc
inherit gettext boot-img

SECTION = "kernel"
SUMMARY = "Android kernel for the Asus Zenwatch"
HOMEPAGE = "https://android.googlesource.com/"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"
COMPATIBLE_MACHINE = "athias"

SRC_URI = "git://github.com/locusf/linux-athias.git;protocol=https \
    file://defconfig \
    file://img_info "
SRCREV = "9abe307cd7ba631ea3b70c1503bcc658b125acaf"
LINUX_VERSION ?= "3.10"
PV = "${LINUX_VERSION}+lollipop"
S = "${WORKDIR}/git"
B = "${S}"

BOOT_PARTITION = "/dev/mmcblk0p11"

# Removes some headers that are installed incorrectly

do_configure_prepend() {
    # Fixes build with GCC5
    echo "#include <linux/compiler-gcc4.h>" > ${S}/include/linux/compiler-gcc5.h
}

do_install_append() {
    rm -rf ${D}/usr/src/usr/
}
