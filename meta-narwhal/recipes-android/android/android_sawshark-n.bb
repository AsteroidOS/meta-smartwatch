inherit gettext

SUMMARY = "Downloads the Huawei Watch 2 /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/w29tu6vvqyii8y3/system-NXH20B.tar.gz \
    file://60-i2c.rules \
"
SRC_URI[md5sum] = "62fd2442be19c3134a2a9fcca7809f52"
SRC_URI[sha256sum] = "6b4473e19f98781f8f6673484c0f94ac87dee6c0ba3700397b685e788f86627e"
PV = "nougat"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "sawshark"
INSANE_SKIP_${PN} = "already-stripped"
S = "${WORKDIR}"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    # Allow pulseaudio to control I2C devices, for speaker.
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/60-i2c.rules ${D}${sysconfdir}/udev/rules.d/

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

    install -d ${D}/
    install -m 644 system/property_contexts ${D}/
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES_android-system = "/system /vendor /usr ${sysconfdir}/udev ${sysconfdir}/Tfa98xx.cnt /property_contexts"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
