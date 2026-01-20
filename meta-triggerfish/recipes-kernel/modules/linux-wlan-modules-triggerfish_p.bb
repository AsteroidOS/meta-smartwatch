SUMMARY = "External WLAN module for triggerfish"
LICENSE = "ISC"
LIC_FILES_CHKSUM = "file://CORE/HDD/inc/wlan_hdd_main.h;beginline=1;endline=26;md5=7692d0637ca32118ead3cb480cff3f2f"
COMPATIBLE_MACHINE = "triggerfish"

inherit module kernel-module-split systemd

SRC_URI = " git://android.googlesource.com/kernel/msm-modules/wlan;branch=android-msm-sole-4.9-pie-wear-mr1;protocol=https \
    file://wlan-module-load.service"
SRCREV = "207571b39e06ee3d8ff7d64d3062eb037a696532"
LINUX_VERSION ?= "4.9"
PV = "${LINUX_VERSION}+pie"
B = "${S}"

DEPENDS = "virtual/kernel virtual/cross-cc"

EXTRA_OEMAKE = " KERNEL_SRC="${STAGING_KERNEL_DIR}" M="${S}""

RPROVIDES:${PN} += "kernel-module-wlan"

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "wlan-module-load.service"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 644 -D ${UNPACKDIR}/wlan-module-load.service ${D}${systemd_system_unitdir}/wlan-module-load.service
}
