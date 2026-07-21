inherit gettext

SUMMARY = "Downloads the TicWatch C2+ system folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://mosushi.de/misc/system-skipjack.tar.gz"
SRC_URI[md5sum] = "4a381613e21a029225aed624b70dc97e"
SRC_URI[sha256sum] = "3e854b7cc1a9caa1209c463f6f0ee7582dbd6d9537a180e55899dd4e4fb97d28"
PV = "pie"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "skipjack"
INSANE_SKIP:${PN} = "ldflags dev-so already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
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
