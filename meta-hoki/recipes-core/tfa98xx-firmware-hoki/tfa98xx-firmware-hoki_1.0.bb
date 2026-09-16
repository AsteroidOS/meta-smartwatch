DESCRIPTION = "Copies the tfa98xx smart speaker amp's onboard DSP firmware \
container into the path the kernel driver's request_firmware() call \
looks under. The real vendor file is present on the stock Android \
/system partition (/system/etc/firmware/tfa98xx.cnt) but nothing \
copies it into AsteroidOS's own /lib/firmware/ at boot."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://tfa98xx-firmware-hoki.sh;beginline=1;endline=2;md5=930f306dfea099b7fea2177393af6bd6"
PR = "r0"

SRC_URI = "file://tfa98xx-firmware-hoki.service \
           file://tfa98xx-firmware-hoki.sh \
           "
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "hoki"

do_install() {
    install -m 0755 -d ${D}${bindir}
    install -m 0755 tfa98xx-firmware-hoki.sh ${D}${bindir}

    install -d ${D}/etc/systemd/system/multi-user.target.wants/
    cp tfa98xx-firmware-hoki.service ${D}/etc/systemd/system/
    ln -s ../tfa98xx-firmware-hoki.service ${D}/etc/systemd/system/multi-user.target.wants/tfa98xx-firmware-hoki.service
}

FILES:${PN} += "/etc/systemd/system/"
