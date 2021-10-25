inherit gettext

SUMMARY = "Downloads the Snapdragon Wear 2100/3100 /usr/libexec/hal-droid and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/j05olh49xqcsdwl/system-sawfish-pie-preview.tar.gz"

SRC_URI[md5sum] = "9c99e1494ee6c90e2584724295f102d2"
SRC_URI[sha256sum] = "6f6678adf70325d23cc3cb89edb1a2fd80eb22455d3d2148eecd9eb7d7fa1028"
PV = "pie"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "skipjack"
INSANE_SKIP_${PN} = "ldflags dev-so already-stripped"
S = "${WORKDIR}"
B = "${S}"

PROVIDES += "virtual/android-system-partition"

do_install() {
    install -d ${D}/system/
    cp -r system/* ${D}/system/

    cd ${D}
    ln -s system/vendor vendor
}

FILES_${PN} = "/system /vendor"
EXCLUDE_FROM_SHLIBS = "1"