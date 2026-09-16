DESCRIPTION = "Loads the hoki analog/digital codec kernel modules before \
audio_machine, which needs their exported msm_anlg_cdc_*/msm_digcdc_* \
symbols."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://audio-firmware-hoki.sh;beginline=1;endline=2;md5=930f306dfea099b7fea2177393af6bd6"
PR = "r0"

SRC_URI = "file://audio-firmware-hoki.service \
           file://audio-firmware-hoki.sh \
           "
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "hoki"

RDEPENDS:${PN} = "kernel-module-audio-analog-cdc kernel-module-audio-digital-cdc kernel-module-audio-machine"

do_install() {
    install -m 0755 -d ${D}${bindir}
    install -m 0755 audio-firmware-hoki.sh ${D}${bindir}

    install -d ${D}/etc/systemd/system/multi-user.target.wants/
    cp audio-firmware-hoki.service ${D}/etc/systemd/system/
    ln -s ../audio-firmware-hoki.service ${D}/etc/systemd/system/multi-user.target.wants/audio-firmware-hoki.service
}

FILES:${PN} += "/etc/systemd/system/"
