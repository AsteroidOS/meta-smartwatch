#!/bin/sh
# Give wlan0 this watch's factory MAC on every boot.
#
# bcmdhd comes up with a random address on Broadcom's test OUI (00:90:4c),
# different on every boot. The factory address is in the device tree as
# chosen/config/wlan_mac1, next to the bt_addr patchram uses. connman's saved
# network id embeds the MAC (wifi_<mac>_<ssid-hex>_managed_psk), so a new MAC
# means saved networks never auto-connect.
#
# macaddr= in bcmdhd.cal is ignored by the driver. Setting the address while
# the interface is down fails with ENODEV, because bcmdhd unregisters the
# netdev on down. It has to be set after bringing the interface up.
set -u

DT=/sys/firmware/devicetree/base/chosen/config/wlan_mac1
IF=wlan0

[ -r "$DT" ] || { echo "sol-wlan-mac: $DT absent -- nothing to apply"; exit 0; }

MAC=$(tr -d '\0' < "$DT" | tr 'A-F' 'a-f')
case "$MAC" in
    [0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]) : ;;
    *) echo "sol-wlan-mac: '$MAC' is not a MAC -- refusing"; exit 0 ;;
esac

# The netdev appears shortly after systemd-modules-load loads the driver.
i=0
while [ "$i" -lt 60 ]; do
    [ -e "/sys/class/net/$IF" ] && break
    sleep 1
    i=$((i + 1))
done
[ -e "/sys/class/net/$IF" ] || { echo "sol-wlan-mac: $IF never appeared"; exit 0; }

cur=$(cat "/sys/class/net/$IF/address" 2>/dev/null)
if [ "$cur" = "$MAC" ]; then
    echo "sol-wlan-mac: $IF already has $MAC"
    exit 0
fi

# The address can only be set while the interface is UP, and at this point it
# is usually soft-blocked, so `ip link set up` fails with RF-kill. Lift the
# block briefly and restore it as found: connman applies the user's WiFi
# preference afterwards, and this must not turn the radio on.
blocked=$(rfkill list wifi 2>/dev/null | grep -c "Soft blocked: yes")
[ "$blocked" -gt 0 ] && rfkill unblock wifi 2>/dev/null

ip link set "$IF" up 2>/dev/null
i=0
while [ "$i" -lt 8 ]; do
    [ "$(cat /sys/class/net/$IF/operstate 2>/dev/null)" != "down" ] && break
    sleep 1
    i=$((i + 1))
done

if ip link set "$IF" address "$MAC" 2>/dev/null; then
    echo "sol-wlan-mac: $IF $cur -> $(cat /sys/class/net/$IF/address)"
else
    echo "sol-wlan-mac: failed to set $MAC on $IF (still $(cat /sys/class/net/$IF/address 2>/dev/null))"
fi

[ "$blocked" -gt 0 ] && rfkill block wifi 2>/dev/null
exit 0
