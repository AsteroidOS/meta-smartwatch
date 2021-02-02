inherit gettext

SUMMARY = "Downloads the Moto 360 2015 /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/y94skkze8aq3l5x/system-MEC23G.tar.gz"
SRC_URI[md5sum] = "fc9e8cb2f7019a6b9b87f0bab0f481e8"
SRC_URI[sha256sum] = "735bdcc3b5689f5d3abee4d26579323ebd5d591c32e73d93e3d102842828a1ae"
PV = "marshmallow"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "smelt"
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
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES_android-system = "/system /vendor /usr ${sysconfdir}/udev"
FILES_android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
