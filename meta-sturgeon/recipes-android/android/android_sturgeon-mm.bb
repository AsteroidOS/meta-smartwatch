inherit gettext

SUMMARY = "Downloads the Huawei Watch /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/j3lbzk349k2qtn6/hybris-m-msm8226.tar.gz;name=hybris \
    https://dl.dropboxusercontent.com/s/j0s11jkpofx72m8/system-sturgeon-m.tar.gz;name=system \
    file://60-i2c.rules \
"
SRC_URI[hybris.md5sum] = "e211970b9d541844916dc2e8a70cca2e"
SRC_URI[hybris.sha256sum] = "baf4b938ab554b0e9941c8f011b2c87a47a9fb0fda64de556ca336479f50fc9f"
SRC_URI[system.md5sum] = "843743dce59007511051513385fa86a8"
SRC_URI[system.sha256sum] = "75388f6fa139c635411669cd67691cdaee04a18e6e809ad480d55dc81df26593"
PV = "marshmallow"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "sturgeon"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    # Allow pulseaudio to control I2C devices, for speaker.
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/60-i2c.rules ${D}${sysconfdir}/udev/rules.d/

    install -d ${D}/system/
    cp -r system/* ${D}/system/

    install -d ${D}/usr/
    cp -r usr/* ${D}/usr/

    install -d ${D}${includedir}/android
    cp -r include/* ${D}${includedir}/android/

    install -d ${D}${libdir}/pkgconfig
    install -m 0644 ${D}${includedir}/android/android-headers.pc ${D}${libdir}/pkgconfig
    rm ${D}${includedir}/android/android-headers.pc
    cd ${D}
    ln -s system/vendor vendor
    # Make symlink for speaker functionality.
    ln -s /system/etc/Tfa98xx.cnt etc/Tfa98xx.cnt
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/system /vendor /usr ${sysconfdir}/udev ${sysconfdir}/Tfa98xx.cnt"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
