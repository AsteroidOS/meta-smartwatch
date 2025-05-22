inherit gettext

SUMMARY = "Provides the modified /system data for ray."
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/al40eklhv50l6z5/system-ray-pie-preview.tar.gz;name=system \
    file://60-i2c.rules \
"
SRC_URI[system.md5sum] = "6f0d6d01a4bd2396f09754211310d0ba"
SRC_URI[system.sha256sum] = "f91af5f3115fc9a5d66ea360b6faead0b458fcdbdeaf84a5eac9cac808d1ccda"
PV = "oreo"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "ray|firefish"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
B = "${S}"

do_install() {
    # Allow pulseaudio to control I2C devices, for speaker.
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/60-i2c.rules ${D}${sysconfdir}/udev/rules.d/

    install -d ${D}/system/
    cp -r system/* ${D}/system/

    cd ${D}
    ln -s system/vendor vendor
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

FILES:${PN} = "${sysconfdir}/udev /system /vendor"
EXCLUDE_FROM_SHLIBS = "1"
