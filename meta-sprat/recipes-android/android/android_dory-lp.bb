inherit gettext

SUMMARY = "Downloads the LG G Watch /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "Closed"
SRC_URI = "http://placeholder.fr/~kido/dory/system.tar.gz"
SRC_URI[md5sum] = "061d11d47e0046019c9a76c8cf31bdd4"
SRC_URI[sha256sum] = "ce74ced2810506e2d344d614851ac4502e5a83d7d6b81c899936fb865c97a27b"
PV = "lollipop"

INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "dory"
S = "${WORKDIR}/system/"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    install -d ${D}/system/
    cp -r system/* ${D}/system/

    install -d ${D}${includedir}/android
    cp -r include/* ${D}${includedir}/android/

    install -d ${D}${libdir}/pkgconfig
    install -m 0644 ${D}${includedir}/android/android-headers.pc ${D}${libdir}/pkgconfig
    rm ${D}${includedir}/android/android-headers.pc
}

PACKAGES =+ "android-system android-headers"
FILES_android-system = "/system"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
