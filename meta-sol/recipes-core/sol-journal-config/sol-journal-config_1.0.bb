SUMMARY = "sol: journald drop-in enabling persistent storage during bring-up"
DESCRIPTION = "Ships /etc/systemd/journald.conf.d/00-sol-persistent-journal.conf. \
Kept as a standalone recipe rather than a systemd bbappend on purpose: systemd is \
a deep dependency, so a bbappend there invalidates systemd and everything below it \
and turns a one-line config change into a multi-thousand-task rebuild. This recipe \
depends on nothing, so editing the drop-in rebuilds exactly one package."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://00-sol-persistent-journal.conf"
S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${sysconfdir}/systemd/journald.conf.d
    install -m 0644 ${UNPACKDIR}/00-sol-persistent-journal.conf \
        ${D}${sysconfdir}/systemd/journald.conf.d/00-sol-persistent-journal.conf
}

FILES:${PN} = "${sysconfdir}/systemd/journald.conf.d"
RDEPENDS:${PN} = "systemd"
