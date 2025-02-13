do_install:append:rinato() {
    install -d ${D}/usr/share/
    install -m 0755 ${WORKDIR}/psplash-img-320.png ${D}/usr/share/psplash.png
}
