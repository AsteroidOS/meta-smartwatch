do_install_append_lenok() {
    sed -i "s,mce.service,," ${D}/usr/lib/systemd/user/asteroid-launcher.service
}
