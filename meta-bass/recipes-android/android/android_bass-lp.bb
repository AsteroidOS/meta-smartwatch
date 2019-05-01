inherit gettext

SUMMARY = "Downloads the LG Watch Urbane /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/9sd5tkb4z34kehq/system-bass.tar.gz"
SRC_URI[md5sum] = "4c37d04332b388504e34dfeefec25eea"
SRC_URI[sha256sum] = "29c4ff7155557ae132c8e2299aa61032ffbf87e1cc8bc05f61c5b22924a8b337"
PV = "lollipop"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "bass"
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
    ln -s system/vendor vendor

    mkdir etc/
    ln -s /system/etc/firmware etc/firmware
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/bass-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES_android-system = "/system /vendor /etc /usr"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
