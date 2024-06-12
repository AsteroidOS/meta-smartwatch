SECTION = "kernel"
SUMMARY = "Firmware for Samsung rinato smartwatch"
HOMEPAGE = "https://github.com/casept/linux-samsung-smartwatch-firmware"
LICENSE = "CLOSED"
COMPATIBLE_MACHINE = "rinato"

SRC_URI = " git://git@github.com/casept/linux-samsung-smartwatch-firmware.git;protocol=https;branch=master \
    file://fake-copying"

# For random firmware files extracted from wherever, the licensing is of course unclear.
# Just create an empty file and checksum that to make yocto shut up.
LIC_FILES_CHKSUM = "file://fake-copying;md5=d41d8cd98f00b204e9800998ecf8427e"

SRC_URI[sha256sum] = "fffffffl33857fl2fa0318a1fac1e788941015bf39894a255b8323c5138037c6"
SRCREV = "fe84f346a9337b5ad4a474895e1a62c52aad7f36"
PV = "master"
S = "${WORKDIR}/git"

FILES:${PN} += " /usr/lib/firmware "

do_install() {
        install -m 0755 -d ${D}/usr/lib/firmware/
        # Touch screen
        install -m 0755 -d ${D}/usr/lib/firmware/tsp_melfas/w/
        install -m 0644 ${S}/rinato/tsp_melfas/w/* ${D}/usr/lib/firmware/tsp_melfas/w/
        # BT / Wi-Fi
        install -m 0755 -d ${D}/usr/lib/firmware/brcm/
        cp ${S}/rinato/brcm/bcm4334W.hcd ${D}/usr/lib/firmware/brcm/BCM.hcd
        cp ${S}/rinato/brcm/brcmfmac43342-sdio.bin ${D}/usr/lib/firmware/brcm/brcmfmac43342-sdio.samsung,rinato.bin
        cp ${S}/rinato/brcm/brcmfmac43342-sdio.txt ${D}/usr/lib/firmware/brcm/brcmfmac43342-sdio.txt
        # Misc. accelerators / codecs
        install -m 0644 ${S}/rinato/s5p* ${D}/usr/lib/firmware/
        # Sensor hub
        install -m 0644 ${S}/rinato/ssp* ${D}/usr/lib/firmware/
}
