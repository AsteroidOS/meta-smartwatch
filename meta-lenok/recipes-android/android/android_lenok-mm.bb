inherit gettext

SUMMARY = "Downloads the LG G Watch R /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://misc.atx.name/lenok-system-v2.tar.gz"
SRC_URI[md5sum] = "132206a5c91302383a06d90b6d8023a0"
SRC_URI[sha256sum] = "47a0daaf028c222ac93f021a49a1c4a882359ad9292039e29ea84a56846357e8"
PV = "marshmallow"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "lenok"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
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
FILES:android-system = "/system /vendor"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
