do_install:append:lenok() {
    sed -i "s,mce.service,," ${D}/usr/lib/systemd/user/asteroid-launcher.service
}
