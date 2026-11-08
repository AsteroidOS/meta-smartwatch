FILESEXTRAPATHS:prepend:nemo := "${THISDIR}/bluetooth-rfkill-event-configs:"
SRC_URI:append:nemo = " file://bcm4343.conf \
    file://bluetooth-rfkill-event"
