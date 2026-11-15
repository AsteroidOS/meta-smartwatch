#!/bin/sh
# Let msm-dwc3 reach low power mode when there is no cable, so the SoC can
# suspend, without losing the gadget recovery that needs the opposite.
#
# soc:google,extcon-usb-shim ships spoof_connected=1 and reports a cable
# whatever the PMIC says. At 1 every suspend aborts:
#     msm-dwc3 4e00000.hsusb: Abort PM suspend!! (USB is outside LPM)
# At a permanent 0 the gadget never re-enumerates after a cable loss, and
# sol-usb-recover's dwc3 unbind/rebind is safe only at 1 (at 0 it resets the
# SoC). So follow the charger: 1 while present, 0 while absent. Clearing it
# with a cable attached is harmless; the shim then follows the PMIC.
#
# Polled, because udev's USB event path is what is broken on this device (see
# sol-bringup-adb).
set -u

SHIM=/sys/bus/platform/devices/soc:google,extcon-usb-shim/spoof_connected
ONLINE=/sys/class/power_supply/usb/online
POLL=5

[ -w "$SHIM" ] || { echo "sol-usb-lpm: $SHIM not writable -- nothing to do"; exit 0; }

last=""
while true; do
    # Absent or unreadable reads as "assume a cable", the safe direction: the
    # cost is a watch that does not suspend, versus one that cannot be reached.
    want=$(cat "$ONLINE" 2>/dev/null) || want=1
    case "$want" in 0|1) ;; *) want=1 ;; esac

    if [ "$want" != "$last" ]; then
        if echo "$want" > "$SHIM" 2>/dev/null; then
            if [ "$want" = "1" ]; then
                echo "sol-usb-lpm: charger present -- spoof_connected=1, gadget recovery available"
            else
                echo "sol-usb-lpm: no charger -- spoof_connected=0, USB may enter LPM"
            fi
            last=$want
        else
            echo "sol-usb-lpm: failed to write $want to spoof_connected"
        fi
    fi
    sleep "$POLL"
done
