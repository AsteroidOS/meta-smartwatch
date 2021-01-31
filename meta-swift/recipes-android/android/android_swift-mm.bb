inherit gettext

SUMMARY = "Downloads the Asus Zenwatch 3 /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "http://kicherer.org/aos/system.tar.gz"
SRC_URI[md5sum] = "60ab67b7b988ee018258744efc547cb7"
SRC_URI[sha256sum] = "fbe6f939e7062bb14f0df4698dddd9c31e1a098ddb3cceecaee18ddb2fc43366"
PV = "marshmallow"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "swift"
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
EXCLUDE_FROM_SHLIBS = "1"
