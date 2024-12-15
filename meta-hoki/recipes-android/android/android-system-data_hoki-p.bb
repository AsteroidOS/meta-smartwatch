inherit gettext

SUMMARY = "Downloads the Fossil Gen 6 system/vendor folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/1mmpew8kyn52jko/system-hoki-p.tar.gz"
SRC_URI[md5sum] = "a1c2efc27d1a2531a34e5c5345964147"
SRC_URI[sha256sum] = "1416a01a8fd19e9c82aa0ca9bde33db6853eb08f5411592b4b178364971417ce"
PV = "pie"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "hoki"
INSANE_SKIP:${PN} = "ldflags dev-so already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
B = "${S}"

do_install() {
    install -d ${D}/system/
    cp -r system/* ${D}/system/

    install -d ${D}/vendor/
    cp -r vendor/* ${D}/vendor/
}

do_package_qa() {
}

FILES:${PN} = "/system /vendor"
EXCLUDE_FROM_SHLIBS = "1"
