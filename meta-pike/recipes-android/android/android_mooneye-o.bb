inherit gettext

SUMMARY = "Downloads the Ticwatch E & S /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/ubcsdhlbv09jxxo/system-pike.tar.gz"
SRC_URI[md5sum] = "c1d225bca2257d13724600b3edac0e44"
SRC_URI[sha256sum] = "9c89cbbd9ececff94467912498b19b239a6b8f6a3a72927cf75d0400e391ac99"
PV = "oreo"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "pike"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
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
}

do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/system /vendor /usr"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
