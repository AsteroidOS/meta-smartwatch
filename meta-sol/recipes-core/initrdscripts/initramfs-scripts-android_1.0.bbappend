FILESEXTRAPATHS:prepend:sol := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:sol = "sol"

# As on aurora, init.sh modprobes the vendor modules early from
# /etc/modules.load.sol: sdhci_msm.ko and ~85 other SoC drivers are =m on GKI.
SRC_URI:append:sol = " file://init.sh file://modules.load.sol"

do_install:append:sol() {
    install -m 0755 ${UNPACKDIR}/init.sh ${D}/init

    # Module list order: geni serial, q6v5_pas, ADSP audio, nanohub, WLAN.
    install -m 0644 -D ${UNPACKDIR}/modules.load.sol ${D}/etc/modules.load.sol
}

FILES:${PN}:append:sol = " /etc/modules.load.sol"
