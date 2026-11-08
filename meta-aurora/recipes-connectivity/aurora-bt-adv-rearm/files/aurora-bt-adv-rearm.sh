#!/bin/sh
# Re-register the BLE advertisement every time Bluetooth is switched back on.
#
# Switching Bluetooth off and on leaves the watch invisible to the phone.
# Power-off ends in an HCI Reset, which wipes the controller's advertising sets.
# On power-on the kernel sends LE Set Extended Advertising Enable (Disabled) for
# handle 1, the controller answers 0x42 Unknown Advertising Identifier, and
# nothing ever creates the set again. bluez still reports ActiveInstances 1, so
# the phone cannot reconnect until asteroid-btsyncd registers its advertisement
# again. Restarting btsyncd does exactly that (MGMT Remove Advertising + Add
# Extended Advertising).
#
# Seen identically on sol and aurora, so the bug is in shared code (kernel
# Bluetooth host / bluez), not in this device. This is a device-scoped
# workaround until it is fixed there; drop it once a toggle advertises again.
set -u

UNIT=asteroid-btsyncd
USER_NAME=ceres

restart_btsyncd() {
    uid=$(id -u "$USER_NAME" 2>/dev/null) || return 0
    # No user session yet (early boot): btsyncd will register fresh when it starts.
    [ -S "/run/user/$uid/bus" ] || return 0
    if su -s /bin/sh "$USER_NAME" -c "XDG_RUNTIME_DIR=/run/user/$uid systemctl --user try-restart $UNIT"; then
        echo "aurora-bt-adv-rearm: Bluetooth powered on -- restarted $UNIT to re-register advertising"
    else
        echo "aurora-bt-adv-rearm: Bluetooth powered on -- restarting $UNIT failed"
    fi
}

# PropertiesChanged on the adapter prints, among others:
#       string "Powered"
#       variant             boolean true
# Act on the line after a "Powered" key; ignore PowerState and the rest.
dbus-monitor --system \
    "type=signal,sender=org.bluez,interface=org.freedesktop.DBus.Properties,member=PropertiesChanged,path=/org/bluez/hci0" |
while read -r line; do
    case "$line" in
        'string "Powered"') key=powered; continue ;;
    esac
    if [ "${key:-}" = powered ]; then
        key=
        case "$line" in
            *"boolean true"*) restart_btsyncd ;;
        esac
    fi
done
# dbus-monitor only exits if the system bus goes away; let systemd restart us.
exit 1
