inherit gettext

SUMMARY = "Downloads the Huawei Watch 2 Bluetooth /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/u1o3mtnlojg49jt/system-NXH20B.tar.gz \
    file://60-i2c.rules \
"
SRC_URI[md5sum] = "23ba5eccf6f1f89e5eb9e01d40b0904d"
SRC_URI[sha256sum] = "4236577c7a28497aa6d4cc672e5f2bcf304231ebd820a1144911a2f21fbfa3f1"
PV = "nougat"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "sawfish"
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
EXCLUDE_FROM_SHLIBS = "1"
