inherit gettext

SUMMARY = "Downloads the TicWatch C2+ system folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dl.dropboxusercontent.com/s/t9kde7nfcqlo1br/Sensors-Skipjack.tar.gz"
SRC_URI[md5sum] = "5be9e5a44de5ae229f3af131810c0578"
SRC_URI[sha256sum] = "a0437c7313e0c7e1a1839567b5b21c420fe5c496422f65b38cccc18c81020b8c"
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
