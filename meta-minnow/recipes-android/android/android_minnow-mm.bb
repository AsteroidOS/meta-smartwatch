inherit gettext

SUMMARY = "Downloads the Moto 360 /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/xoaew2dktn4clol/hybris-m-omap3.tar.gz;name=hybris \
        https://dl.dropboxusercontent.com/s/8p1uo6t52glcel7/firmware-minnow.tar.gz;name=firmware"
SRC_URI[hybris.md5sum] = "20b5aa706abe56f31d976ed7c5946f9b"
SRC_URI[hybris.sha256sum] = "c9eecae73dcdf222e27d8b6d31f133e0842c6551dbacac38ab3f97d4476d0e81"
SRC_URI[firmware.md5sum] = "c9807256b6a40673b3a62453d6196ff7"
SRC_URI[firmware.sha256sum] = "87e35b6848f4af1a08a287b6ef193e610ee3a69f15aeb2668b85e101b1a523ee"
PV = "marshmallow"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "minnow"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    install -d ${D}/firmware/
    cp -r firmware/* ${D}/firmware/

    install -d ${D}/usr/
    cp -r usr/* ${D}/usr/

    install -d ${D}${includedir}/android
    cp -r include/* ${D}${includedir}/android/

    install -d ${D}${libdir}/pkgconfig
    install -m 0644 ${D}${includedir}/android/android-headers.pc ${D}${libdir}/pkgconfig
    rm ${D}${includedir}/android/android-headers.pc
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/usr /firmware"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
