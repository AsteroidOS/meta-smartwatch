SUMMARY = "Keep the unconnected serial console of the TicWatch E/S from stalling"
DESCRIPTION = "The bootloader makes ttyMT1 the kernel console. Nothing is \
connected to it and it picks up line noise: a serial getty reads that noise as \
login attempts and enables XON/XOFF flow control, so a stray ^S can stop \
console output for good. systemd then blocks writing status lines to the \
console and the watch hangs, usually during shutdown. Mask the getty, turn \
flow control off and stop systemd writing status lines to the console."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://20-no-console-status.conf \
           file://console-no-ixon.service"
S = "${UNPACKDIR}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "mooneye"

do_install() {
    install -d ${D}${sysconfdir}/systemd/system.conf.d
    install -m 0644 20-no-console-status.conf ${D}${sysconfdir}/systemd/system.conf.d/

    install -d ${D}${sysconfdir}/systemd/system/sysinit.target.wants
    install -m 0644 console-no-ixon.service ${D}${sysconfdir}/systemd/system/
    ln -s ../console-no-ixon.service ${D}${sysconfdir}/systemd/system/sysinit.target.wants/console-no-ixon.service

    ln -s /dev/null ${D}${sysconfdir}/systemd/system/serial-getty@ttyMT1.service
}

FILES:${PN} = "${sysconfdir}/systemd"
