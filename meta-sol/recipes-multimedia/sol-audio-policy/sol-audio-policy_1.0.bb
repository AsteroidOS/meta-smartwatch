SUMMARY = "sol: derive PulseAudio's droid audio policy configuration"
DESCRIPTION = "module-droid-card needs an audio policy configuration it can both \
find and survive. sol's A14 vendor keeps its copy in a per-SKU directory below \
every path libdroid-util searches, and declares LE Audio device ports whose \
device types are newer than the module -- building the default profile set over \
them segfaults PulseAudio. This ships a boot-time filter that copies the vendor \
policy without those ports, plus the default.pa line pointing at the result. \
The second half is a workaround for shared code, kept device-scoped on purpose: \
every A12+ vendor image declares these ports, so whether the module should skip \
device types it does not know -- which would retire this recipe -- is a fleet \
decision rather than one this port should take."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-audio-policy.sh \
           file://sol-audio-policy.service"

S = "${UNPACKDIR}"

inherit systemd

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-audio-policy.sh ${D}${libexecdir}/sol-audio-policy.sh
    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-audio-policy.service ${D}${systemd_unitdir}/system/sol-audio-policy.service
}

FILES:${PN} = "${libexecdir}/sol-audio-policy.sh \
               ${systemd_unitdir}/system/sol-audio-policy.service"

RDEPENDS:${PN} = "sol-vendor-mount"

SYSTEMD_SERVICE:${PN} = "sol-audio-policy.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
