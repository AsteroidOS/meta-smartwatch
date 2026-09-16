DESCRIPTION = "Sets the ALSA mixer routing controls hoki's speaker and \
microphone need to actually route audio through DAPM, after waiting \
for both ALSA card enumeration and PulseAudio's own startup probing to \
settle (both were found to reset/race these controls if set too early)."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://audio-mixer-fix-hoki.sh;beginline=1;endline=2;md5=930f306dfea099b7fea2177393af6bd6"
PR = "r0"

SRC_URI = "file://audio-mixer-fix-hoki.service \
           file://audio-mixer-fix-hoki.sh \
           "
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "hoki"

RDEPENDS:${PN} = "audio-firmware-hoki alsa-utils-amixer"

do_install() {
    install -m 0755 -d ${D}${bindir}
    install -m 0755 audio-mixer-fix-hoki.sh ${D}${bindir}

    install -d ${D}/etc/systemd/system/multi-user.target.wants/
    cp audio-mixer-fix-hoki.service ${D}/etc/systemd/system/
    ln -s ../audio-mixer-fix-hoki.service ${D}/etc/systemd/system/multi-user.target.wants/audio-mixer-fix-hoki.service
}

FILES:${PN} += "/etc/systemd/system/"
