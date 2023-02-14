DESCRIPTION = "Load the WiFi firmware and enable driver"
PR = "r0"
SRC_URI = "file://wifi-enabler.service"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"
S = "${WORKDIR}"

PACKAGE_ARCH = "${MACHINE_ARCH}"

do_install() {
    install -d ${D}/lib/systemd/system/multi-user.target.wants/
    cp ${WORKDIR}/wifi-enabler.service ${D}/lib/systemd/system/
    ln -s ../wifi-enabler.service ${D}/lib/systemd/system/multi-user.target.wants/wifi-enabler.service
}

FILES:${PN} += "/lib/systemd/system/"
