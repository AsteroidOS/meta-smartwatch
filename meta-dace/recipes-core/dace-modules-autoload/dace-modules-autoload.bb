SUMMARY = "dace: post-rootfs modules (autoload list + vendor blacklist)"
DESCRIPTION = "The .ko's in /lib/modules/<krel>/vendor/ (linux-dace-modules) \
are only reachable AFTER the switch_root to the rootfs (the VKB ramdisk only \
carries the boot-critical ones), so their load goes through \
systemd-modules-load.d/dace-post-rootfs.conf. \
NOTE: that file used to be shipped WITH THE LIST COMMENTED OUT because that \
module chain (WLAN/icnss2 + ASoC + BT) took the SoC down to EDL at ~10 s: the \
real trigger is udev's coldplug by modalias, but systemd-modules-load does an \
explicit 'modprobe <module>' and 'blacklist' does NOT block that (only \
aliases). That is why /etc/modprobe.d/00-dace-vendor-blacklist.conf is also \
installed (76 modules, blocks udev) and why the autoload list had to stay \
commented out until the culprit was bisected: uncommenting lines is how it is \
bisected."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "dace"

# 2026-09-18: the list is NO LONGER commented out. It was verified that kmod
# applies the blacklist as a *deny-list* also to systemd-modules-load's
# `modprobe` ("Module 'wlan' is deny-listed (by kmod)"), so that service cannot
# load the chain. dace-modules-load.service loads it by hand, doing an explicit
# `modprobe` (which DOES ignore the deny-list) reading the same
# /etc/modules-load.d/dace-post-rootfs.conf.
inherit systemd

SRC_URI = "file://dace-post-rootfs.conf \
           file://dace-vendor-blacklist.conf \
           file://dace-modules-load.sh \
           file://dace-modules-load.service"

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "dace-modules-load.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d -m 0755 ${D}${sysconfdir}/modules-load.d
    install -m 0644 ${UNPACKDIR}/dace-post-rootfs.conf ${D}${sysconfdir}/modules-load.d/dace-post-rootfs.conf
    # The vendor modules are loaded by udev (not modules-load) and take the SoC
    # down: they are blocked with modprobe.d (see the file's own comment).
    install -d -m 0755 ${D}${sysconfdir}/modprobe.d
    install -m 0644 ${UNPACKDIR}/dace-vendor-blacklist.conf ${D}${sysconfdir}/modprobe.d/00-dace-vendor-blacklist.conf
    # Explicit load (the blacklist deny-list blocks systemd-modules-load).
    install -d -m 0755 ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/dace-modules-load.sh ${D}${libexecdir}/dace-modules-load.sh
    install -d -m 0755 ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/dace-modules-load.service ${D}${systemd_system_unitdir}/dace-modules-load.service
}

FILES:${PN} = "${sysconfdir}/modules-load.d/dace-post-rootfs.conf \
               ${sysconfdir}/modprobe.d/00-dace-vendor-blacklist.conf \
               ${libexecdir}/dace-modules-load.sh \
               ${systemd_system_unitdir}/dace-modules-load.service"

RDEPENDS:${PN} = "linux-dace-modules"
