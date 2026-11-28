SUMMARY = "sol: boot the modem subsystem, where the GNSS engine lives"
DESCRIPTION = "Nothing starts 6080000.remoteproc-mss at boot, so geoclue's provider waits \
for a GNSS engine that is not running. This binds and starts the modem with \
the firmware loader pointed at firmware_mnt/image, then restores the path. \
Ships disabled: positioning against this modem used to reset the SoC, and what \
changed since is not isolated."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://sol-modem-boot.sh \
           file://sol-modem-boot.service"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-modem-boot.sh ${D}${libexecdir}/sol-modem-boot.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-modem-boot.service \
        ${D}${systemd_unitdir}/system/sol-modem-boot.service
}

SYSTEMD_SERVICE:${PN} = "sol-modem-boot.service"
# Not auto-enabled: started at boot it puts sol into a reset loop (uptime never
# past ~55 s). Started by hand on a running system it has not reset. The
# difference is not understood; find it before enabling this.
# Test GPS by hand: /usr/libexec/sol-modem-boot.sh
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

FILES:${PN} = "${libexecdir}/sol-modem-boot.sh \
               ${systemd_unitdir}/system/sol-modem-boot.service"
