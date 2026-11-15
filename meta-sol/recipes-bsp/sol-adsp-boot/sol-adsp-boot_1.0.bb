SUMMARY = "sol: bring up the ADSP for the Qualcomm USF sensor sub-HAL"
DESCRIPTION = "sol's sensors need both the Google MCU (nanohub) and the Qualcomm ADSP, \
where the USF sub-HAL named in /vendor/etc/sensors/hals.conf runs. The kernel's \
firmware loader only searches /lib/firmware, which holds no adsp.*, so the ADSP \
remoteproc stayed offline and sscrpcd looped trying to spawn the sensor PD. \
This points the loader at the vendor image's firmware and starts the remoteproc."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-adsp-boot.sh \
           file://sol-adsp-boot.service"

S = "${UNPACKDIR}"

inherit systemd

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-adsp-boot.sh ${D}${libexecdir}/sol-adsp-boot.sh
    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-adsp-boot.service ${D}${systemd_unitdir}/system/sol-adsp-boot.service
}

FILES:${PN} = "${libexecdir}/sol-adsp-boot.sh \
               ${systemd_unitdir}/system/sol-adsp-boot.service"

RDEPENDS:${PN} = "sol-vendor-mount"

SYSTEMD_SERVICE:${PN} = "sol-adsp-boot.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
