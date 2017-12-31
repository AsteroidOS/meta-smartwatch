FILESEXTRAPATHS_prepend_swift := "${THISDIR}/${PN}:"
SRC_URI_append_swift = " file://70-input.rules"

do_install_append_swift() {
	install -m 0644 ${WORKDIR}/70-input.rules ${D}${sysconfdir}/udev/rules.d/70-input.rules
}
