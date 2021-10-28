inherit gettext

SUMMARY = "Downloads the Snapdragon Wear 2100/3100 /usr/libexec/hal-droid and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/yl8tezw6e2d661o/hybris-o-msm8909.tar.gz \
    file://60-i2c.rules \
"
SRC_URI[md5sum] = "7891147518b1c1a3071af6173c9fd38f"
SRC_URI[sha256sum] = "61c59dbcb894e693c0fd092d690efdb5fa63fd5784be63e0e3749f25af800ce8"
PV = "oreo"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "ray"
INSANE_SKIP_${PN} = "already-stripped"
S = "${WORKDIR}"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    # Allow pulseaudio to control I2C devices, for speaker.
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/60-i2c.rules ${D}${sysconfdir}/udev/rules.d/

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
FILES_android-system = "/vendor /usr ${sysconfdir}/udev ${sysconfdir}/Tfa98xx.cnt"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
