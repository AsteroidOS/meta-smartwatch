do_install_append_lenok() {
    sed -i "s,/dev/input/event0,/dev/input/event1," ${D}/var/lib/environment/compositor/default.conf
    sed -i "s,mce.service,," ${D}/usr/lib/systemd/user/asteroid-launcher.service
}
