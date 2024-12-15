DESCRIPTION = "When boot is done, we can underclock the CPU and GPU to save energy"
PR = "r0"
SRC_URI = "file://underclock.service \
           file://underclock \
           file://COPYING"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=84dcc94da3adb52b53ae4fa38fe49e5d"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "sturgeon"

do_install() {
    install -m 0755 -d ${D}${bindir}
    install -m 0755 underclock ${D}${bindir}

    install -d ${D}/etc/systemd/system/default.target.wants/
    cp underclock.service ${D}/etc/systemd/system/
    ln -s ../underclock.service ${D}/etc/systemd/system/default.target.wants/underclock.service
}

