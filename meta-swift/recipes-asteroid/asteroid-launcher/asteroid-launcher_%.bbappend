do_install_append_swift() {
    sed -i "s,/dev/input/event0,/dev/input/touch0," ${D}/var/lib/environment/compositor/default.conf
    sed -i "s,mce.service,," ${D}/usr/lib/systemd/user/asteroid-launcher.service
}
