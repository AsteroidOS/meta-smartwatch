FILESEXTRAPATHS:prepend:swift := "${THISDIR}/${PN}:"
SRC_URI:append:swift = " file://70-input.rules"

do_install:append:swift() {
	install -m 0644 ${WORKDIR}/70-input.rules ${D}${sysconfdir}/udev/rules.d/70-input.rules
}
