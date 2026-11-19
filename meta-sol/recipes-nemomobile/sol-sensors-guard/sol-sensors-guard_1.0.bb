SUMMARY = "Re-register sol's HIDL sensors HAL when sensorfw's D-Bus goes deaf"
DESCRIPTION = "sol ships a HIDL sensors bridge (sol-sensors-hidl) because its \
A14 vendor exposes only an AIDL sensors HAL while AsteroidOS's sensorfw is a \
HIDL client. After an abnormal container restart the HIDL HAL can end up \
unregistered, leaving sensorfw's HybrisManager::finishConnect() spinning in a \
blocking sleep() on its main thread -- its D-Bus interface then answers nothing, \
mce's sensor plugin loads pile up past the pending-reply cap and jam mce, and \
the display can no longer be woken with the power key. This guard probes \
sensorfw's D-Bus every few minutes and, only when it is genuinely wedged, \
re-runs the container bring-up so the bridge re-applies and the HAL registers. \
It does nothing on a healthy watch."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"
COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-sensors-guard.sh \
           file://sol-sensors-guard.service \
           file://sol-sensors-guard.timer"

inherit systemd

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "mce dbus-tools"

SYSTEMD_SERVICE:${PN} = "sol-sensors-guard.timer"
SYSTEMD_AUTO_ENABLE = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/sol-sensors-guard.sh ${D}${bindir}/

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/sol-sensors-guard.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${UNPACKDIR}/sol-sensors-guard.timer ${D}${systemd_system_unitdir}/
}

FILES:${PN} = "${bindir}/sol-sensors-guard.sh \
               ${systemd_system_unitdir}/sol-sensors-guard.service \
               ${systemd_system_unitdir}/sol-sensors-guard.timer"
