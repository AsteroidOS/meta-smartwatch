SUMMARY = "QCA BT firmware for the TicWatch Pro 5 (dace)"
# Extracted from the stock 'bluetooth' partition (/dev/mmcblk0p18, FAT16):
#   /image/apbtfw11.tlv + apnv11.bin   -> WCN3988 'ap' family
#   /image/slbtfw20.mbn + slnv20.bin   -> 'sl' family (the slate-mode one)
#
# NOTE (2026-09-20): with the SOC in slate mode (DT compatible qcom,qcc5100,
# which is what stock ships) the Qualcomm HAL requests the **sl** family and
# these two files must be in the container's /vendor/firmware (copied by
# dace-lxc-android-start.sh from /lib/firmware/qca):
#   File open /vendor/firmware/slbtfw20.mbn succeeded   <- patch (376 segments)
#   File open /vendor/firmware/slnv20.bin  succeeded    <- NVM   (17 segments)
# Without them the chip starts (answers Get Version) but does not receive the
# patch and the HAL dies with 'Controller Init failed'. The 'ap' family is kept
# in case the btattach/btqca path (cherokee soc) is used some day.
DESCRIPTION = "QCA BT firmware (WCN3988) for the TicWatch Pro 5: ap and sl families, extracted from the bluetooth p18 partition"
LICENSE = "CLOSED"
COMPATIBLE_MACHINE = "dace"

SRC_URI = "file://qca/apbtfw11.tlv \
           file://qca/apnv11.bin \
           file://qca/slbtfw20.mbn \
           file://qca/slnv20.bin"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware/qca
    install -m 0644 ${UNPACKDIR}/qca/apbtfw11.tlv ${D}${nonarch_base_libdir}/firmware/qca/
    install -m 0644 ${UNPACKDIR}/qca/apnv11.bin  ${D}${nonarch_base_libdir}/firmware/qca/
    install -m 0644 ${UNPACKDIR}/qca/slbtfw20.mbn ${D}${nonarch_base_libdir}/firmware/qca/
    install -m 0644 ${UNPACKDIR}/qca/slnv20.bin  ${D}${nonarch_base_libdir}/firmware/qca/
}

FILES:${PN} = "${nonarch_base_libdir}/firmware/qca/*"
