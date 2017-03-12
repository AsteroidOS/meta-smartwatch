inherit gettext

SUMMARY = "Downloads the LG G Watch R /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://misc.atx.name/lenok-system.tar.gz"
SRC_URI[md5sum] = "3aad6275150e5eb3b22df2be05db6dde"
SRC_URI[sha256sum] = "7be10e57ae1645afe29c76bc3ca66f0ea07b672070270209f38fb1294f22a56f"
PV = "marshmallow"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "lenok"
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

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES_android-system = "/system /vendor"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
