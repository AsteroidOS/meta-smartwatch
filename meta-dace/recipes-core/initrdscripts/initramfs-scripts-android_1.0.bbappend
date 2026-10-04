FILESEXTRAPATHS:prepend:dace := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:dace = "dace"

# Dace uses an early modprobe loop from /etc/modules.load.dace. The loop is
# required because dace's eMMC driver (sdhci_msm.ko) and ~85 other vendor
# drivers are =m on this GKI kernel.
SRC_URI:append:dace = " file://init.sh file://modules.load.dace file://google-extcon-usb-shim.conf"

do_install:append:dace() {
    install -m 0755 ${UNPACKDIR}/init.sh ${D}/init

    # Dependency-ordered module list (msm_geni_serial -> q6v5_pas chain -> ADSP
    # audio chain -> nanohub MCU -> WLAN), matching what UBPorts uses.
    install -m 0644 -D ${UNPACKDIR}/modules.load.dace ${D}/etc/modules.load.dace

    # google-extcon-usb-shim defaults to USB force-disable=1 from DT ("USB
    # force-disable:1 changeable:1 dt-support:1 disable-param:0x1"), which
    # prevents dwc3-msm from ever creating a UDC -> no USB peripheral, no adb.
    # Override via modprobe.d so the param applies the moment the busybox
    # modprobe loop loads the shim module.
    install -m 0644 -D ${UNPACKDIR}/google-extcon-usb-shim.conf ${D}/etc/modprobe.d/google-extcon-usb-shim.conf
}

FILES:${PN}:append:dace = " /etc/modules.load.dace /etc/modprobe.d/google-extcon-usb-shim.conf"
