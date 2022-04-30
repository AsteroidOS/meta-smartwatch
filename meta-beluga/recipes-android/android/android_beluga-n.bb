inherit gettext

SUMMARY = "Downloads the Snapdragon Wear 2100/3100 /usr/libexec/hal-droid and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/8b9t2renrxbl4gq/hybris-o-msm8909.tar.gz;name=hybris \
    https://dl.dropboxusercontent.com/s/4n595u8giwe8z66/system-beluga-o.tar.gz;name=system \
    file://60-i2c.rules \
"
SRC_URI[hybris.md5sum] = "edc1f8304b58af335a9c9ba8136bc1b8"
SRC_URI[hybris.sha256sum] = "626bed275cfe2df2377e709498fc26d58e7883045cd13d4e2a6284220d1113b0"
SRC_URI[system.md5sum] = "9b8a27701f7ee1f8b4247253cebab9d6"
SRC_URI[system.sha256sum] = "9c8aa12ed8873ec989ee055debca2c606f066da5553f7d63b41a12525e5dd7aa"
PV = "oreo"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "beluga"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    # Allow pulseaudio to control I2C devices, for speaker.
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/60-i2c.rules ${D}${sysconfdir}/udev/rules.d/

    install -d ${D}/usr/
    cp -r usr/* ${D}/usr/

    install -d ${D}${includedir}/android
    cp -r include/* ${D}${includedir}/android/

    install -d ${D}${libdir}/pkgconfig
    install -m 0644 ${D}${includedir}/android/android-headers.pc ${D}${libdir}/pkgconfig
    rm ${D}${includedir}/android/android-headers.pc

    install -d ${D}/system/
    cp -r system/* ${D}/system/

    cd ${D}
    ln -s system/vendor vendor
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/usr ${sysconfdir}/udev /system /vendor"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
