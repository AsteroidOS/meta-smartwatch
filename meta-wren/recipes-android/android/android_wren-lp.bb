inherit gettext

SUMMARY = "Downloads the Asus ZenWatch 2 /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://landau.fi/2020/asteroid/wren/system.tar.gz"
SRC_URI[md5sum] = "6671debe2bee41e247ac3a1a76fe0430"
SRC_URI[sha256sum] = "37b111ea9962b277c5edf964964b1de211f870e1e0b2dfcc0d163a53ed8668a6"
PV = "lollipop"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "wren"
INSANE_SKIP_${PN} = "already-stripped"
S = "${WORKDIR}"
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
    cd ${D}
    ln -s system/vendor vendor
}

do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES_android-system = "/system /vendor"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
