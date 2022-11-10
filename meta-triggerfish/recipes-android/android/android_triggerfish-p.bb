inherit gettext

SUMMARY = "Downloads the Snapdragon Wear 2100 /usr/libexec/hal-droid and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/xhtzxu3i1rj550x/hybris-p-msm8909.tar.gz;name=hybris \
    https://dl.dropboxusercontent.com/s/yppldo214trtd7n/libmcutool.so;name=libmcetool \
    file://60-i2c.rules \
"
SRC_URI[hybris.md5sum] = "498f109103d8442ad9c0308da9109cc1"
SRC_URI[hybris.sha256sum] = "a13c40696d905076f71a5edc9810c40e628428c6eb4d4bd46d66b9c0c705db4d"
SRC_URI[libmcetool.md5sum] = "b821a26b7299edc5a144818bb8279799"
SRC_URI[libmcetool.sha256sum] = "0099b6c16feb9ceef56a5e87d9ff293aed810b827cf1a9c33f6f2ef3e6b3b59d"
PV = "pie"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "triggerfish"
INSANE_SKIP:${PN} = "already-stripped"
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

    install -d ${D}/usr/libexec/hal-droid/system/lib/
    install -m 644 ${WORKDIR}/libmcutool.so ${D}/usr/libexec/hal-droid/system/lib/
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/usr ${sysconfdir}/udev /usr/libexec/hal-droid/system/lib/"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
