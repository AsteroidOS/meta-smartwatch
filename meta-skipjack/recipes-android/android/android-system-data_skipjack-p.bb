inherit gettext

SUMMARY = "Downloads the TicWatch C2+ system folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/scl/fi/40t4h39rzn4b4o7inc67g/system-skipjack.tar.gz?rlkey=s1pexo5gg82l0jt9tk34mdhgw&dl=0;downloadfilename=system-skipjack.tar.gz"
SRC_URI[md5sum] = "fc4c198e3b6c3594bb53a411c38a81d3"
SRC_URI[sha256sum] = "3d0a4a902680f3226dc67263477642a2c674dd5ced41545182416ee5103d456d"
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
