SUMMARY = "sol: keep the nanohub shared buffer pool from being exhausted"
DESCRIPTION = "google-modules/nanohub shares one pool of 20 buffers (READ_QUEUE_DEPTH) \
across all 30 channels, and a buffer only returns to the pool on a userspace \
read(). The drop-if-nobody-listens guard in nanohub_process_buffer() and the \
discard in nanohub_release() only cover ID_NANOHUB_CLIENT. The other userspace \
channels queue unconditionally and AsteroidOS opens none of them, so twenty \
unread messages on any one strand the pool and the driver stops reading the \
MCU: every sensor dies. \
\
That starts the known lockup chain: sensors stall, sensorfwd stops answering \
D-Bus, mce blocks in an untimed D-Bus call while holding a wakelock and the \
watch cannot suspend. \
\
Device-scoped workaround: hold those nodes open and discard what arrives. The \
right fix is in the driver, which aurora shares."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://sol-nanohub-drain.sh \
           file://sol-nanohub-drain.service"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/sol-nanohub-drain.sh ${D}${bindir}/sol-nanohub-drain.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-nanohub-drain.service \
        ${D}${systemd_unitdir}/system/sol-nanohub-drain.service
}

SYSTEMD_SERVICE:${PN} = "sol-nanohub-drain.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "${bindir}/sol-nanohub-drain.sh \
               ${systemd_unitdir}/system/sol-nanohub-drain.service"
