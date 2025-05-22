inherit gettext

SUMMARY = "Downloads the LG G Watch /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/j3lbzk349k2qtn6/hybris-m-msm8226.tar.gz;name=hybris \
    https://dl.dropboxusercontent.com/s/pny53u172fyheh4/system-dory-m.tar.gz;name=system \
"
SRC_URI[hybris.md5sum] = "e211970b9d541844916dc2e8a70cca2e"
SRC_URI[hybris.sha256sum] = "baf4b938ab554b0e9941c8f011b2c87a47a9fb0fda64de556ca336479f50fc9f"
SRC_URI[system.md5sum] = "5069fb9da1f987c113ea626d45e294eb"
SRC_URI[system.sha256sum] = "b9b5759af1c9bd73fddddaa61f2339a08796a06fa343d2d2ffde7234af83b8fc"
PV = "marshmallow"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "dory"
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

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/system /vendor /usr"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
