SUMMARY = "External audio modules for beluga"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://NOTICE;md5=53c09804050a00b1d27bd609c4e1fc5a"
COMPATIBLE_MACHINE = "hoki"

inherit module kernel-module-split

SRC_URI = "git://github.com/fossil-engineering/kernel-msm-fossil-extra-cw-audiokernel;branch=fossil-android-msm-hoki-lw1.2-4.14;protocol=https \
           file://0001-Remove-export-from-Kbuild-files.patch \
           file://0002-Avoid-shell-expansion-in-recursively-expanded-variab.patch \
           file://0003-Import-beluga-makefile.patch \
           file://0004-Ignore-compilation-warnings.patch \
           file://0005-dsp-Compile-codecs-for-out-of-tree-builds.patch \
           "
SRCREV = "c984389253fc58bc316af06bf3504dd2c25382be"
LINUX_VERSION ?= "4.9"
PV = "${LINUX_VERSION}+pie"
B = "${S}"

DEPENDS = "virtual/kernel"

EXTRA_OEMAKE = " KERNEL_SRC="${STAGING_KERNEL_DIR}" M="${S}""

RPROVIDES:${PN} += "kernel-module-audio-apr"
RPROVIDES:${PN} += "kernel-module-audio-pinctrl-wcd"
RPROVIDES:${PN} += "kernel-module-audio-swr-ctrl"
RPROVIDES:${PN} += "kernel-module-audio-swr"
RPROVIDES:${PN} += "kernel-module-audio-q6"
RPROVIDES:${PN} += "kernel-module-audio-adsp-loader"
RPROVIDES:${PN} += "kernel-module-audio-usf"
RPROVIDES:${PN} += "kernel-module-audio-q6-notifier"
RPROVIDES:${PN} += "kernel-module-audio-machine"
RPROVIDES:${PN} += "kernel-module-audio-stub"
RPROVIDES:${PN} += "kernel-module-audio-wsa881x"
RPROVIDES:${PN} += "kernel-module-audio-mbhc"
RPROVIDES:${PN} += "kernel-module-audio-wcd-core"
RPROVIDES:${PN} += "kernel-module-audio-wsa881x-analog"
RPROVIDES:${PN} += "kernel-module-audio-wcd9xxx"
RPROVIDES:${PN} += "kernel-module-audio-platform"
