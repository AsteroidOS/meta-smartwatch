inherit gettext

SUMMARY = "Downloads specific vendor files that are not easily available for the Ticwatch Pro 3."
LICENSE = "CLOSED"
SRC_URI = "https://www.dropbox.com/scl/fi/a07m1g61893ur0tahhp51/libmcutool.so?rlkey=asg9hjhmhh0jy6bdqys6o9zjw&st=r4313z4b&dl=1;name=libmcutool"
SRC_URI[libmcutool.md5sum] = "041ff9a6067cf3eb3d6c5aa04262f9a1"
SRC_URI[libmcutool.sha256sum] = "d6d4ba661e5e6da1551422eed7e2a0858d14f36d9e3f6c053bb064f642a8dedb"
PV = "pie"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "rubyfish"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
B = "${S}"

do_install() {
    install -d ${D}/usr/libexec/hal-droid/system/lib/
    install -m 644 ${UNPACKDIR}/libmcutool.so ${D}/usr/libexec/hal-droid/system/lib/
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/dory-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system-extras"
FILES:android-system = "${sysconfdir}/udev /usr/libexec/hal-droid/system/lib/"
EXCLUDE_FROM_SHLIBS = "1"
