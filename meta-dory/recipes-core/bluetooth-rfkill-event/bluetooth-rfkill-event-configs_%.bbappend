FILESEXTRAPATHS:prepend:dory := "${THISDIR}/bluetooth-rfkill-event-configs:"
SRC_URI:append:dory = " file://bcm4343.conf \
    file://bluetooth-rfkill-event"
