# The MT6630 firmware cannot hold LE connections in the peripheral role (the
# link drops with a connection timeout ~400 ms after LE Connection Complete,
# on every kernel including the stock Wear OS one). Stock Wear OS paired with
# phones over BR/EDR. Enable BR/EDR so phones can pair and run GATT over it.
do_install:append:mooneye() {
    conf=${D}${sysconfdir}/bluetooth/main.conf
    if ! grep -q '^ControllerMode = le$' $conf; then
        bbfatal "main.conf: no 'ControllerMode = le' line; mooneye quirk not applied"
    fi
    sed -i 's/^ControllerMode = le$/ControllerMode = dual/' $conf
}
