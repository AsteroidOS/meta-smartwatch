DESCRIPTION = "Scatter file for MTK6580 watches"
LICENSE = "PD"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/PD;md5=b3597d12946881e13cb3b548d1173851"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
SRC_URI = "file://MT6580_AsteroidOS_scatter.txt"

S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"

inherit deploy kernel-artifact-names

PACKAGE_ARCH = "${MACHINE_ARCH}"

do_install() {
	install -d ${D}/boot
	install ${S}/MT6580_AsteroidOS_scatter.txt ${D}/boot/
    sed -i "s@%%KERNEL%%@zImage-dtb-${MACHINE}.fastboot@" ${D}/boot/MT6580_AsteroidOS_scatter.txt
    sed -i "s@%%ROOTFS%%@asteroid-image-${MACHINE}.ext4@" ${D}/boot/MT6580_AsteroidOS_scatter.txt
    sed -i "s@%%LOGO%%@logo-${MACHINE}.bin@" ${D}/boot/MT6580_AsteroidOS_scatter.txt
}

do_deploy() {
    base_name=MT6580_AsteroidOS_scatter-${MACHINE}${IMAGE_VERSION_SUFFIX}.txt
    symlink_name=MT6580_AsteroidOS_scatter-${MACHINE}.txt
    cp ${D}/boot/MT6580_AsteroidOS_scatter.txt ${DEPLOY_DIR_IMAGE}/${base_name}

    ln -sf ${base_name} ${DEPLOY_DIR_IMAGE}/${symlink_name}
}

addtask deploy before do_build after do_install

do_deploy[depends] += "mtk6580-asteroid-logo:do_deploy"

FILES:${PN} += "/boot/MT6580_AsteroidOS_scatter.txt"
