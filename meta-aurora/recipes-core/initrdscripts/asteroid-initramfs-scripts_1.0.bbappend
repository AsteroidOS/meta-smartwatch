FILESEXTRAPATHS:prepend:aurora := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:aurora = "aurora"

# Aurora uses an early modprobe loop from /etc/modules.load.aurora. The loop is
# required because aurora's eMMC driver (sdhci_msm.ko) and ~85 other vendor
# drivers are =m on this GKI kernel.
SRC_URI:append:aurora = " file://init.sh file://modules.load.aurora file://google-extcon-usb-shim.conf"

do_install:append:aurora() {
    install -m 0755 ${UNPACKDIR}/init.sh ${D}/init

    # Dependency-ordered module list (msm_geni_serial -> q6v5_pas chain -> ADSP
    # audio chain -> nanohub MCU -> WLAN), matching what UBPorts uses.
    install -m 0644 -D ${UNPACKDIR}/modules.load.aurora ${D}/etc/modules.load.aurora

    # google-extcon-usb-shim defaults to USB force-disable=1 from DT ("USB
    # force-disable:1 changeable:1 dt-support:1 disable-param:0x1"), which
    # prevents dwc3-msm from ever creating a UDC -> no USB peripheral, no adb.
    # Override via modprobe.d so the param applies the moment the busybox
    # modprobe loop loads the shim module.
    install -m 0644 -D ${UNPACKDIR}/google-extcon-usb-shim.conf ${D}/etc/modprobe.d/google-extcon-usb-shim.conf
}

FILES:${PN}:append:aurora = " /etc/modules.load.aurora /etc/modprobe.d/google-extcon-usb-shim.conf"
