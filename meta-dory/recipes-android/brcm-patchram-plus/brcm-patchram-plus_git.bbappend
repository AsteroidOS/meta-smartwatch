FILESEXTRAPATHS:prepend:dory := "${THISDIR}/brcm-patchram-plus:"
SRC_URI:append:dory = " file://patchram.service "
CFLAGS:append:dory = " -DLPM_DORY"

do_install:append:dory() {
    # Disable service file from starting.
    rm -rf ${D}/lib/systemd/system/multi-user.target.wants/patchram.service
    # Remove the service file as starting it is handled by bluetooth-rfkill-event.
    rm -rf ${D}/lib/systemd/system/patchram.service
}