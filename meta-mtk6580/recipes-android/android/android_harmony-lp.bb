SUMMARY = "Downloads the harmony /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/eslcib5qxz9cmtx/system-harmony.tar.xz"
SRC_URI[md5sum] = "e17dc083ce94e333bbf0395936edd7fc"
SRC_URI[sha256sum] = "6a44badc6224e0205e4ea65642cdb05219419a79758ac1aac41f5bd8eccb9f48"
PV = "lollipop"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "harmony"
INSANE_SKIP_${PN} = "already-stripped"
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
    ln -s /system/vendor vendor

    mkdir etc/
    ln -s /system/etc/firmware etc/firmware

    chmod +x ${D}/system/bin/*
}

do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES_android-system = "/system /vendor /usr /etc/firmware"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
