SUMMARY = "sol: make the geoclue binder provider able to reach the GNSS HAL"
DESCRIPTION = "Two settings stand between geoclue-provider-hybris-binder and \
sol's AIDL GNSS HAL, neither of them the provider's fault: libgbinder defaults to \
Android 9's servicemanager dialect, so the AIDL lookup finds nothing on an \
Android 14 container, and the provider's own unit sets ProtectHome=yes, which \
hides the session bus socket under /run/user and kills it before it starts. Each \
file here carries the measurement behind it."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://sol-gbinder.conf \
           file://50-sol-protecthome.conf"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${sysconfdir}/gbinder.d
    install -m 0644 ${UNPACKDIR}/sol-gbinder.conf ${D}${sysconfdir}/gbinder.d/sol.conf

    install -d ${D}${systemd_unitdir}/user/geoclue-providers-hybris.service.d
    install -m 0644 ${UNPACKDIR}/50-sol-protecthome.conf \
        ${D}${systemd_unitdir}/user/geoclue-providers-hybris.service.d/50-sol-protecthome.conf
}

FILES:${PN} = "${sysconfdir}/gbinder.d/sol.conf \
               ${systemd_unitdir}/user/geoclue-providers-hybris.service.d/50-sol-protecthome.conf"

RDEPENDS:${PN} = "libgbinder geoclue-provider-hybris-binder"
