DESCRIPTION = "Utility to set correct time on LG Watch W7"
HOMEPAGE = "https://github.com/AsteroidOS/narwhal-hands-timesync.git"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=84dcc94da3adb52b53ae4fa38fe49e5d"

SRC_URI = "git://github.com/asteroidos/narwhal-hands-timesync.git;branch=main;protocol=https"
SRCREV = "${AUTOREV}"
PR = "r0"
PV = "+git${SRCPV}"
S = "${WORKDIR}/git"
DEPENDS = "qtbase"
inherit cmake_qt5
PACKAGE_ARCH = "${MACHINE_ARCH}"

do_install:append() {
    install -d ${D}/etc/systemd/system/timers.target.wants/
    ln -s ${systemd_system_unitdir}/hands-timesync.timer ${D}/etc/systemd/system/timers.target.wants/hands-timesync.timer
}

FILES:${PN} += "${systemd_system_unitdir} /etc/systemd/"
