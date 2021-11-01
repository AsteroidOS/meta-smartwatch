inherit gettext

SUMMARY = "Downloads the TicWatch C2+ system folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/xihp73hhfm5m4ky/Amalgamated-Skipjack-System.tar.gz"
SRC_URI[md5sum] = "61756e5e971eb0d3a8d925f4eb9230da"
SRC_URI[sha256sum] = "d458178a2909077a107b93d7e589d6ba336958e2953f779ea7b7e7f722055177"
PV = "pie"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "skipjack"
INSANE_SKIP:${PN} = "ldflags dev-so already-stripped"
S = "${WORKDIR}"
B = "${S}"

PROVIDES += "virtual/android-system-partition"

do_install() {
    install -d ${D}/system/
    cp -r system/* ${D}/system/

    cd ${D}
    ln -s system/vendor vendor
}

do_package_qa() {
}

FILES:${PN} = "/system /vendor"
EXCLUDE_FROM_SHLIBS = "1"
