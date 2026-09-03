DESCRIPTION = "Symlinks the real vendor GNSS HAL binary to the filename AsteroidOS's \
hal-droid init compat layer actually looks for, and enables NTP-based AGPS \
assistance data injection over WiFi (this device has no cellular modem for \
the alternate SUPL assistance path)."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://gps_xtra.ini;md5=2dfad6f123b25b9c584cc209770743a2"
PR = "r0"

SRC_URI = "file://gps_xtra.ini"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "hoki"

do_install() {
    install -m 0755 -d ${D}${sysconfdir}
    install -m 0644 gps_xtra.ini ${D}${sysconfdir}/gps_xtra.ini
}

pkg_postinst:${PN}() {
    # hal-droid's init .rc parses a service filename without the "-qti"
    # suffix, but the real vendor binary is only present as
    # android.hardware.gnss@1.0-service-qti. Symlink so the service
    # actually starts. See https://github.com/AsteroidOS/meta-smartwatch/issues/252
    ln -sf android.hardware.gnss@1.0-service-qti $D/vendor/bin/hw/android.hardware.gnss@1.0-service
}

FILES:${PN} += "${sysconfdir}/gps_xtra.ini"
