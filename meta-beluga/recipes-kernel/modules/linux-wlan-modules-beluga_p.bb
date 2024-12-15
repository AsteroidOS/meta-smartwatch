SUMMARY = "External WLAN module for beluga"
LICENSE = "ISC"
LIC_FILES_CHKSUM = "file://CORE/HDD/inc/wlan_hdd_main.h;beginline=1;endline=26;md5=7692d0637ca32118ead3cb480cff3f2f"
COMPATIBLE_MACHINE = "beluga"

inherit module kernel-module-split systemd

SRC_URI = " git://android.googlesource.com/kernel/msm-modules/wlan;branch=android-msm-beluga-4.9-pie-wear-mr2;protocol=https \
    file://wlan-module-load.service"
SRCREV = "28c211183f34f6a96a03f88ce6ac53b497a0653e"
LINUX_VERSION ?= "4.9"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

DEPENDS = "virtual/kernel virtual/${TARGET_PREFIX}gcc"

EXTRA_OEMAKE = " KERNEL_SRC="${STAGING_KERNEL_DIR}" M="${S}""

RPROVIDES:${PN} += "kernel-module-wlan"

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "wlan-module-load.service"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 644 -D ${UNPACKDIR}/wlan-module-load.service ${D}${systemd_system_unitdir}/wlan-module-load.service
}
