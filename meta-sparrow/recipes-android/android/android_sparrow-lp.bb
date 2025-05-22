inherit gettext

SUMMARY = "Downloads the Asus ZenWatch 2 /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/j3lbzk349k2qtn6/hybris-m-msm8226.tar.gz;name=hybris \
    https://dl.dropboxusercontent.com/s/0vcryg0esbz7698/system-sparrow-m.tar.gz;name=system \
"
SRC_URI[hybris.md5sum] = "e211970b9d541844916dc2e8a70cca2e"
SRC_URI[hybris.sha256sum] = "baf4b938ab554b0e9941c8f011b2c87a47a9fb0fda64de556ca336479f50fc9f"
SRC_URI[system.md5sum] = "581ddf4a53dfa9a350d561cf7ddabdc0"
SRC_URI[system.sha256sum] = "1c35baddfd28033f92faadda03bc4070397c8c22c4f95257e266a434ca0fb14a"

PV = "lollipop"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "sparrow"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
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

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/sparrow-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/system /vendor /usr"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
