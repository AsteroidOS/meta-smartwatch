DESCRIPTION = "This installs an extra service which loads /system/bin/servicemanager"
PR = "r0"
SRC_URI = "file://reinit-servicemanager.service"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"
S = "${WORKDIR}"

PACKAGE_ARCH = "${MACHINE_ARCH}"

do_install() {
    install -d ${D}/lib/systemd/system/multi-user.target.wants/

    cp ${WORKDIR}/reinit-servicemanager.service ${D}/lib/systemd/system/
    ln -s ../reinit-servicemanager.service ${D}/lib/systemd/system/multi-user.target.wants/reinit-servicemanager.service
}

FILES_${PN} += "/lib/systemd/system/"