#!/bin/sh
# Put sol's Broadcom WLAN (synadhd4381) into suspend power-save mode while the
# display is off, as Wear OS does: SETSUSPENDMODE 1 at screen-off, 0 at
# screen-on. Suspend mode raises the DTIM listen interval (bcn_li_dtim 1 -> 9)
# and enables the packet filters, so the chip wakes the host far less often.
#
# Not done per suspend attempt from a PM notifier: the SDIO commands raise
# wakeup events that abort the attempt, and sol never suspends with Wi-Fi on.
#
# wl-privcmd fails harmlessly while Wi-Fi is off.
set -u
dbus-monitor --system \
    "type='signal',interface='com.nokia.mce.signal',member='display_status_ind'" |
while read -r line; do
    case "$line" in
        *'string "off"'*) wl-privcmd wlan0 "SETSUSPENDMODE 1" >/dev/null 2>&1 ;;
        *'string "on"'*)  wl-privcmd wlan0 "SETSUSPENDMODE 0" >/dev/null 2>&1 ;;
    esac
done
