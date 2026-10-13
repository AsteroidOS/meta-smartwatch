FILESEXTRAPATHS:prepend:minnow := "${THISDIR}/mce:"
SRC_URI:append:minnow = " file://60-m4sensorhub-evdev.ini"

do_install:append:minnow() {
    install -m 0644 ${UNPACKDIR}/60-m4sensorhub-evdev.ini ${D}${sysconfdir}/mce/60-m4sensorhub-evdev.ini
}
