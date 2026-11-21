FILESEXTRAPATHS:prepend:mooneye := "${THISDIR}/usb-moded:"

# The android gadget driver here names the network interface usb0, not
# rndis0, so developer mode (SSH over USB) never got an address.
do_install:append:mooneye() {
    ini=${D}${sysconfdir}/usb-moded/dyn-modes/developer_mode.ini
    if ! grep -q '^network_interface = rndis0$' $ini; then
        bbfatal "developer_mode.ini: no 'network_interface = rndis0' line; mooneye quirk not applied"
    fi
    sed -i 's/^network_interface = rndis0$/network_interface = usb0/' $ini
}
