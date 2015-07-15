inherit gettext

SUMMARY = "Downloads the LG G Watch /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "Closed"
SRC_URI = "http://placeholder.fr/~kido/dory/system.tar.xz"
PV = "lollipop"

INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "dory"
S = "${WORKDIR}/system/"
B = "${S}"

FILESEXTRAPATHS_prepend := "${THISDIR}/files-lp:"
SRC_URI += "file://extract-headers.sh"
SRC_URI[md5sum] = "cf648a3ee9682b34d681352a7b3c95cc"
SRC_URI[sha256sum] = "e797b5ae862e94e5eee19b04b904206cb44b43b89a156e6824e2b21203bfaf94"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    cp -r system ${D}/system/

    install -d ${D}${includedir}/android
    cp -r include/* ${D}${includedir}/android/

    install -d ${D}${libdir}/pkgconfig
    install -m 0644 ${D}${includedir}/android/android-headers.pc ${D}${libdir}/pkgconfig
    rm ${D}${includedir}/android/android-headers.pc
}

PACKAGES =+ "android-system android-headers"
FILES_android-system = "/system"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
