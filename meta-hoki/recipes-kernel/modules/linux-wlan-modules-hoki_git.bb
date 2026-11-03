SUMMARY = "WLAN (Prima/pronto) kernel module for hoki"
DESCRIPTION = "Out-of-tree Qualcomm WCNSS/pronto WLAN driver for hoki (SDA429W, msm8937), \
built from the LA.UM.10.6.2.r1-02500-89xx.0 prima tree with a fix for a real \
use-before-assignment bug in hdd_process_bt_sco_profile() that oopses the kernel \
whenever Bluetooth is active while the module loads. \
See https://github.com/AsteroidOS/meta-smartwatch/issues/224"
LICENSE = "ISC"
LIC_FILES_CHKSUM = "file://CORE/HDD/src/wlan_hdd_main.c;beginline=1;endline=13;md5=7e85947c06d7ceca827c1a3ca1182f78"
COMPATIBLE_MACHINE = "hoki"

inherit module kernel-module-split

SRC_URI = "git://github.com/Dev-msm8953/vendor_qcom_opensource_wlan_prima;protocol=https;branch=LA.UM.10.6.2.r1-02500-89xx.0 \
           file://0001-hdd_process_bt_sco_profile-fix-use-of-adapter-before.patch \
           file://0002-Makefile-quote-CC-LD-AR-OBJCOPY-STRIP-in-nested-make.patch \
           "
SRCREV = "3401b53"
LINUX_VERSION ?= "4.14"
PV = "${LINUX_VERSION}+git"
S = "${WORKDIR}/git"
B = "${S}"

DEPENDS = "virtual/kernel"

EXTRA_OEMAKE = " KERNEL_SRC="${STAGING_KERNEL_DIR}" KERNEL_SOURCE="${STAGING_KERNEL_DIR}" M="${S}" CONFIG_PRONTO_WLAN=m WLAN_ROOT="${S}" MODNAME=wlan"

RPROVIDES:${PN} += "kernel-module-wlan"
