SUMMARY = "sol: load the CS40L26 haptic driver once its firmware path exists"
DESCRIPTION = "sol's vibrator is a Cirrus CS40L26A LRA haptic driver on i2c \
0x43, confirmed from the device's own DTS: compatible \"cirrus,cs40l26a\" with \
LRA parameters (cirrus,f0-default, cirrus,redc-default, cirrus,vibe-state and \
svc-le2/le3 calibration). It is NOT the speaker amp -- and note sol has no \
wsa883x node at all in any of its DTS (base, shipped or merged); the codec is \
besbev. \
\
The driver is built by linux-sol-modules (cl_dsp-core, cs40l26-core, \
cs40l26-i2c). It needed a hand-written Kbuild because upstream ships only \
BUILD.bazel -- that build-system gap, not missing hardware, is why it was \
previously unbuilt. \
\
No firmware is packaged: cs40l26.wmfw / cs40l26.bin / cs40l26-svc.bin and the \
calibration blobs are already in the Android vendor image, and sol-adsp-boot \
points firmware_class at /android/vendor/firmware, which the kernel searches \
before /lib/firmware. Hence this unit's ordering -- the driver uses synchronous \
request_firmware() and fails permanently if it probes too early. \
\
INTEGRATION IS NOT SOLVED. The driver registers a force-feedback input device \
named cs40l26_input supporting FF_PERIODIC and FF_CUSTOM, and deliberately \
STRIPS FF_RUMBLE. AsteroidOS ships only ngfd-plugin-droid-vibrator (Android HAL \
path) and ngfd-plugin-pulse -- there is no native evdev/FF backend, so nothing \
in userspace will drive this yet. Loading the module proves the hardware; wiring \
it to ngfd is a separate decision and touches shared code."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://sol-haptics.service \
           file://sol-cs40l26-blacklist.conf"
S = "${UNPACKDIR}"

RDEPENDS:${PN} = "linux-sol-modules"

do_install() {
    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-haptics.service \
        ${D}${systemd_unitdir}/system/sol-haptics.service

    install -d ${D}${sysconfdir}/modprobe.d
    install -m 0644 ${UNPACKDIR}/sol-cs40l26-blacklist.conf \
        ${D}${sysconfdir}/modprobe.d/sol-cs40l26.conf
}

SYSTEMD_SERVICE:${PN} = "sol-haptics.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "${systemd_unitdir}/system/sol-haptics.service \
               ${sysconfdir}/modprobe.d/sol-cs40l26.conf"
