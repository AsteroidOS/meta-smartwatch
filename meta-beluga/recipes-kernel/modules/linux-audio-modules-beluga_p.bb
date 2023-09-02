SUMMARY = "External audio modules for beluga"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://NOTICE;md5=53c09804050a00b1d27bd609c4e1fc5a"
COMPATIBLE_MACHINE = "beluga"

inherit module kernel-module-split

SRC_URI = " git://android.googlesource.com/kernel/msm-extra;branch=android-msm-beluga-4.9-pie-wear-mr2;protocol=https \
        file://0001-Make-warnings-non-fatal.patch \
        file://0002-Avoid-shell-expansion-in-recursively-expanded-variab.patch \
        file://0003-Remove-export-from-Kbuild-files.patch \
"
SRCREV = "9c56e30ea127e2c88188db5e2c1637e346ae75a3"
LINUX_VERSION ?= "4.9"
PV = "${LINUX_VERSION}+pie"
S = "${WORKDIR}/git"
B = "${S}"

DEPENDS = "virtual/kernel"

EXTRA_OEMAKE = " KERNEL_SRC="${STAGING_KERNEL_DIR}" M="${S}""

RPROVIDES:${PN} += "kernel-module-audio-adsp-loader"
RPROVIDES:${PN} += "kernel-module-audio-analog-cdc"
RPROVIDES:${PN} += "kernel-module-audio-apr"
RPROVIDES:${PN} += "kernel-module-audio-cpe-lsm"
RPROVIDES:${PN} += "kernel-module-audio-digital-cdc"
RPROVIDES:${PN} += "kernel-module-audio-hdmi"
RPROVIDES:${PN} += "kernel-module-audio-machine-ext"
RPROVIDES:${PN} += "kernel-module-audio-machine-int"
RPROVIDES:${PN} += "kernel-module-audio-machine-msm8909"
RPROVIDES:${PN} += "kernel-module-audio-machine-msm8909-bg"
RPROVIDES:${PN} += "kernel-module-audio-mbhc"
RPROVIDES:${PN} += "kernel-module-audio-native"
RPROVIDES:${PN} += "kernel-module-audio-pinctrl-wcd"
RPROVIDES:${PN} += "kernel-module-audio-platform"
RPROVIDES:${PN} += "kernel-module-audio-q6"
RPROVIDES:${PN} += "kernel-module-audio-q6-notifier"
RPROVIDES:${PN} += "kernel-module-audio-stub"
RPROVIDES:${PN} += "kernel-module-audio-swr"
RPROVIDES:${PN} += "kernel-module-audio-swr-ctrl"
RPROVIDES:${PN} += "kernel-module-audio-tfa98xx"
RPROVIDES:${PN} += "kernel-module-audio-usf"
RPROVIDES:${PN} += "kernel-module-audio-wcd9335"
RPROVIDES:${PN} += "kernel-module-audio-wcd9xxx"
RPROVIDES:${PN} += "kernel-module-audio-wcd-core"
RPROVIDES:${PN} += "kernel-module-audio-wcd-cpe"
RPROVIDES:${PN} += "kernel-module-audio-wglink"
RPROVIDES:${PN} += "kernel-module-audio-wsa881x"
RPROVIDES:${PN} += "kernel-module-audio-wsa881-analog"
