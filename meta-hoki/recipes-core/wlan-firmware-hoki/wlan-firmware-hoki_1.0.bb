DESCRIPTION = "Symlinks stock vendor WCNSS firmware and loads the pronto WLAN driver on hoki"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://wlan-firmware-hoki.sh;beginline=1;endline=2;md5=930f306dfea099b7fea2177393af6bd6"
PR = "r0"

SRC_URI = "file://wlan-firmware-hoki.service \
           file://wlan-firmware-hoki.sh \
           "
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "hoki"

RDEPENDS:${PN} = "kernel-module-wlan"

do_install() {
    install -m 0755 -d ${D}${bindir}
    install -m 0755 wlan-firmware-hoki.sh ${D}${bindir}

    install -d ${D}/etc/systemd/system/multi-user.target.wants/
    cp wlan-firmware-hoki.service ${D}/etc/systemd/system/
    ln -s ../wlan-firmware-hoki.service ${D}/etc/systemd/system/multi-user.target.wants/wlan-firmware-hoki.service
}

FILES:${PN} += "/etc/systemd/system/"
