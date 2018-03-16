DESCRIPTION = "This installs a config file to make usb-moded work on wren"
PR = "r0"
SRC_URI = "file://add-broken-args.conf"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"
S = "${WORKDIR}"

PACKAGE_ARCH = "${MACHINE_ARCH}"

do_install() {
    install -d ${D}/var/lib/environment/usb-moded/

    cp ${WORKDIR}/add-broken-args.conf ${D}/var/lib/environment/usb-moded/
}

FILES_${PN} += "/var/lib/environment/usb-moded/"