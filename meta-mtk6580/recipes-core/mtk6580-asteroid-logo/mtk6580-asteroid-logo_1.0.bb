DESCRIPTION = "AsteroidOS bootsplash for MTK6580 watches"
LICENSE = "CC-BY-SA-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-SA-2.0;md5=d91509a59f42bb5341a8af8295f28211"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
SRC_URI = "git://github.com/AsteroidOS/mtk6580-asteroid-logo.git;protocol=https;branch=master"
SRCREV = "e1612901b9f160d6b2d7d0c2872bb33595f83b60"

inherit deploy image-artifact-names

PACKAGE_ARCH = "${MACHINE_ARCH}"

do_compile() {
    ./pack-logo.pl
}

do_deploy() {
    mkdir -p ${DEPLOY_DIR_IMAGE}

    base_name=logo-${MACHINE}${IMAGE_VERSION_SUFFIX}.bin
    symlink_name=logo-${MACHINE}.bin

    cp ${S}/logo.bin ${DEPLOY_DIR_IMAGE}/${base_name}
    ln -sf ${base_name} ${DEPLOY_DIR_IMAGE}/${symlink_name}
}

addtask deploy before do_build after do_install
