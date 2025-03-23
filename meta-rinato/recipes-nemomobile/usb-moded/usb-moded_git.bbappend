FILESEXTRAPATHS:prepend:rinato := "${THISDIR}/usb-moded:"

SRC_URI:append:rinato = " file://usb-moded.ini "

do_install:append:rinato() {
        mkdir -p ${D}/var/lib/usb-moded/
	cp ${WORKDIR}/usb-moded.ini ${D}/var/lib/usb-moded/usb-moded.ini
}
