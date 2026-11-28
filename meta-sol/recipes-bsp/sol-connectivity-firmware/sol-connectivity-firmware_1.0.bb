SUMMARY = "sol: Broadcom/Synaptics BCM4381 WiFi + Bluetooth firmware"
DESCRIPTION = "Firmware for sol's Broadcom/Synaptics BCM4381 WiFi and Bluetooth combo, \
extracted from the device's stock vendor image. sol does not use the Qualcomm \
WCN part aurora has. \
\
The firmware is shipped as-is while the kernel modules are built from source: \
firmware is loaded at runtime and does not depend on the kernel ABI. Stock's \
modules do: about half their symbol CRCs differ from our kernel's, and they \
reference CFI symbols our kernel does not export. \
\
Installs to /lib/firmware, the kernel's default search path."

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://README.txt;md5=${SOL_FW_README_MD5}"
SOL_FW_README_MD5 = "fd984496b11aa9bb63f9f2c1528053f5"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

# Vendor firmware blobs: none of our toolchain QA applies.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"
EXCLUDE_FROM_SHLIBS = "1"
INSANE_SKIP:${PN} = "already-stripped arch ldflags dev-so file-rdeps textrel staticdev libdir"

SRC_URI = "https://mowerk.net/asteroidos/sol/sol-connectivity-firmware-1.0.tar.gz"
SRC_URI[sha256sum] = "93e316f096342326adefc13408db92645b456cd39f448654c010ce69a182b449"
SRC_URI[md5sum] = "624bcf64a7ffe18d2d06d63f6209fdb9"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware
    # BT patchram: nitrous/btbcm request it by name at HCI setup time.
    install -m 0644 ${UNPACKDIR}/BCM4381A1.hcd     ${D}${nonarch_base_libdir}/firmware/
    install -m 0644 ${UNPACKDIR}/BCM4381A1_SIP.hcd ${D}${nonarch_base_libdir}/firmware/
    # btbcm asks the firmware loader for 'brcm/BCM.hcd', not the vendor name.
    # brcm_patchram_plus uses an explicit path; this covers btattach/serdev.
    install -d ${D}${nonarch_base_libdir}/firmware/brcm
    install -m 0644 ${UNPACKDIR}/BCM4381A1.hcd ${D}${nonarch_base_libdir}/firmware/brcm/BCM.hcd
    install -m 0644 ${UNPACKDIR}/BCM4381A1.hcd ${D}${nonarch_base_libdir}/firmware/brcm/BCM4381A1.hcd
    # WLAN: firmware + regulatory blob + calibration. The _FL22/_FSE22 cal
    # variants are board-specific; ship all three and let the driver pick.
    install -m 0644 ${UNPACKDIR}/fw_bcmdhd.bin     ${D}${nonarch_base_libdir}/firmware/
    install -m 0644 ${UNPACKDIR}/bcmdhd_clm.blob   ${D}${nonarch_base_libdir}/firmware/
    install -m 0644 ${UNPACKDIR}/bcmdhd.cal        ${D}${nonarch_base_libdir}/firmware/
    install -m 0644 ${UNPACKDIR}/bcmdhd.cal_FL22   ${D}${nonarch_base_libdir}/firmware/
    install -m 0644 ${UNPACKDIR}/bcmdhd.cal_FSE22  ${D}${nonarch_base_libdir}/firmware/
    install -d ${D}${datadir}/sol-connectivity-firmware
    install -m 0644 ${UNPACKDIR}/README.txt ${D}${datadir}/sol-connectivity-firmware/README.txt
}

FILES:${PN} = "${nonarch_base_libdir}/firmware \
               ${datadir}/sol-connectivity-firmware"
