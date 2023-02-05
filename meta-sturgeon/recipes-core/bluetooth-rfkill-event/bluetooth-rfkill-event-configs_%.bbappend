FILESEXTRAPATHS:prepend:sturgeon := "${THISDIR}/bluetooth-rfkill-event-configs:"
SRC_URI:append:sturgeon = " file://bcm4343.conf \
    file://bluetooth-rfkill-event"
