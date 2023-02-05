FILESEXTRAPATHS:prepend:sturgeon := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:sturgeon = " file://patchram.service "
CFLAGS:append:sturgeon = " -DLPM_STURGEON"

do_install:append:sturgeon() {
    # Disable service file from starting.
    rm -rf ${D}/lib/systemd/system/multi-user.target.wants/patchram.service
    # Remove the service file as starting it is handled by bluetooth-rfkill-event.
    rm -rf ${D}/lib/systemd/system/patchram.service
}
