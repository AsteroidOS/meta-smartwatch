SUMMARY = "Provides a compatible audio policy for pulseaudio-modules-droid."
LICENSE = "CLOSED"

SRC_URI = "file://audio_policy.conf"
PV = "oreo"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "narwhal"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
B = "${S}"

do_install() {
    # The stock audio policy contains invalid entries that cause the droid module to fail.
    install -d ${D}${sysconfdir}/pulse
    install -m 0644 ${UNPACKDIR}/audio_policy.conf ${D}${sysconfdir}/pulse/
}

FILES:${PN} = "${sysconfdir}/pulse/"
