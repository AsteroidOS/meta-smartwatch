inherit gettext

SUMMARY = "Downloads the Sony Smartwatch3 /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/ts0j8c3et40cnol/system-tetra.tar.gz"
SRC_URI[md5sum] = "b7f52dacf32516d8b99a2e6103941090"
SRC_URI[sha256sum] = "2288c52aef9cdb58839f515c86a908e1d62261295e075cb780660038a1ed43a7"
PV = "lollipop"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "tetra"
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
