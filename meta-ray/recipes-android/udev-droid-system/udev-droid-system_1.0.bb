SUMMARY = "Creates the /dev/block/platform/msm_sdcc.1/by-name/ partitions as expected by some Android binaries."
LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "file://998-droid-system.rules"

S = "${WORKDIR}"
B = "${S}"

do_install_append() {
    install -d ${D}/lib/udev/rules.d
    install -m 644 ${WORKDIR}/998-droid-system.rules ${D}/lib/udev/rules.d/
}
