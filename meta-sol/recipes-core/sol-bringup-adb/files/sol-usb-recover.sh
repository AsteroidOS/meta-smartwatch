#!/bin/sh
# BRING-UP ONLY, ships alongside sol-bringup-adb.sh.
#
# On sol a VBUS loss is a one-way door for USB. sol-bringup-adb.sh binds the UDC
# once, and usb-moded is masked (its udev monitor dies with "udev io watch
# disabled"), so nothing re-binds the gadget after VBUS returns. The host sees
# no device, and only the fastboot button combo recovers. Every off-charger test
# needs a VBUS cut, so without this each one costs a manual recovery.
#
# /sys/class/udc/<udc>/state reports the link state. When VBUS is present and
# the state stays wrong for several samples, re-bind and escalate.
#
# Conservative, because rebinding a healthy gadget drops a live adb session:
#   - acts only with VBUS present,
#   - needs NEED consecutive bad samples,
#   - does nothing while the state is "configured".
set -u

G=/config/usb_gadget/g1
POLL=15
NEED=3          # consecutive bad samples before acting (~45 s)
bad=0
attempts=0    # rebind attempts made; drives the backoff below

# Log to the journal and to /var/log, which survives the reboot and journald
# rotation. This unit diagnoses a state in which adb is gone.
LOGF=/var/log/sol-usb-recover.log
log() {
    echo "sol-usb-recover: $1"
    echo "$(cut -d. -f1 /proc/uptime)s $1" >> "$LOGF" 2>/dev/null || true
}

# The VBUS gate (usb/online, checked in the main loop) is essential. Off charger
# the UDC correctly sits at "not attached", which the loop would count as bad and
# escalate to a dwc3 unbind. Unbinding dwc3 with no cable resets the SoC
# instantly and leaves no trace:
#
#     sol-usb-recover: recovery level 2
#     sol-usb-recover:   dwc3: unbinding 4e00000.dwc3
#     dwc3 4e00000.dwc3: request 0000000000000000 was not queued to ep0out
#     <stream ends, watch reboots>
#
# The same unbind with a cable attached completes cleanly. usb/online tracks
# the cable reliably:
#     plugged:   usb/online=1  qcom_usb/online=1  status=Charging
#     unplugged: usb/online=0  qcom_usb/online=0  status=Discharging
vbus_report() {
    out=""
    for p in /sys/class/power_supply/usb/online \
             /sys/class/power_supply/qcom_usb/online \
             /sys/class/power_supply/sw5100_bms/status; do
        [ -r "$p" ] && out="$out $(basename "$(dirname "$p")")/$(basename "$p")=$(cat "$p" 2>/dev/null)"
    done
    echo "$out"
}

udc_name() { ls /sys/class/udc 2>/dev/null | head -n 1; }

# Bind the gadget. Delegates to sol-bringup-adb.sh so the wait-for-ep1 +
# symlink + bind sequence has one implementation.
bind_gadget() {
    u=$(udc_name)
    [ -n "$u" ] || { log "no UDC present"; return 1; }
    echo "" > "$G/UDC" 2>/dev/null
    systemctl restart android-tools-adbd 2>/dev/null || true
    if [ -x /usr/bin/sol-bringup-adb.sh ]; then
        /usr/bin/sol-bringup-adb.sh 2>&1 | while read -r l; do log "  bringup: $l"; done
    else
        i=0
        while [ ! -e /dev/usb-ffs/adb/ep1 ] && [ "$i" -lt 100 ]; do i=$((i + 1)); sleep 0.1; done
        echo "$u" > "$G/UDC" 2>/dev/null || log "bare bind failed"
    fi
}

# Force the D+ pull-up off and on again. Accepts either the string or the
# numeric form depending on kernel vintage.
soft_reconnect() {
    u=$(udc_name); f="/sys/class/udc/$u/soft_connect"
    [ -e "$f" ] || { log "  no soft_connect node"; return 1; }
    { echo disconnect > "$f" 2>/dev/null || echo 0 > "$f" 2>/dev/null; } && log "  soft_connect: disconnected"
    sleep 2
    { echo connect > "$f" 2>/dev/null || echo 1 > "$f" 2>/dev/null; } && log "  soft_connect: connected"
}

# Full controller re-init: unbind and re-bind the dwc3 platform driver, which
# re-runs the controller's probe and PHY init.
dwc3_reinit() {
    u=$(udc_name)
    d=/sys/bus/platform/drivers/dwc3
    [ -d "$d" ] || { log "  no dwc3 driver dir"; return 1; }
    log "  dwc3: unbinding $u"
    echo "$u" > "$d/unbind" 2>/dev/null || { log "  dwc3 unbind FAILED"; return 1; }
    sleep 3
    log "  dwc3: rebinding $u"
    echo "$u" > "$d/bind" 2>/dev/null || { log "  dwc3 bind FAILED"; return 1; }
    sleep 3
}

# Escalating recovery. After a real VBUS cut a gadget rebind succeeds but the
# state stays "default" (bound, never enumerated): the controller did not
# survive losing VBUS, and only a dwc3 re-init fixes it.
#   L1 gadget rebind + soft_connect          -> state stays "default"
#   L2 dwc3 unbind/bind, then L1             -> "not attached" -> "configured"
rebind() {
    lvl=$1
    log "recovery level $lvl"
    # Level 2 is gated, because a dwc3 unbind in the wrong state resets the SoC:
    #
    #   spoof_connected must be 1. Every clean unbind had spoof=1; both that
    #   reset the watch had spoof=0. The extcon shim state is not a substitute:
    #   with spoof=0 and a cable it follows the PMIC and reads 1.
    #
    #   A host must be attached, not just a charger. usb/type reads USB_CDP or
    #   "USB" (an SDP) on a host port. On a power bank or wall charger (DCP)
    #   usb/online is 1 but the UDC can never reach "configured", so without this
    #   the loop would unbind dwc3 every couple of minutes.
    #
    # Skipping level 2 costs adb recovery; running it wrongly costs the watch.
    spoof=$(cat /sys/bus/platform/devices/soc:google,extcon-usb-shim/spoof_connected 2>/dev/null)
    utype=$(cat /sys/class/power_supply/usb/type 2>/dev/null)
    case "$lvl" in
        1)  bind_gadget; soft_reconnect ;;
        *)  if [ "$spoof" != "1" ]; then
                log "  SKIPPING dwc3 re-init: spoof_connected='$spoof', unbind is only proven safe at 1"
            elif [ "$utype" != "USB" ] && [ "${utype#*CDP}" = "$utype" ] && [ "${utype#*SDP}" = "$utype" ]; then
                log "  SKIPPING dwc3 re-init: usb/type='$utype' is not a host port"
            else
                dwc3_reinit
            fi
            bind_gadget; soft_reconnect ;;
    esac
    u=$(udc_name)
    sleep 5
    log "post-recovery UDC state='$(cat "/sys/class/udc/$u/state" 2>/dev/null)' UDC='$(cat "$G/UDC" 2>/dev/null)'"
}

[ -d "$G" ] || { log "FATAL: $G missing"; exit 1; }
log "watchdog started (poll=${POLL}s, threshold=${NEED} samples)"

last_report=0
while true; do
    sleep "$POLL"
    u=$(udc_name)
    [ -n "$u" ] || continue
    st=$(cat "/sys/class/udc/$u/state" 2>/dev/null)

    # The VBUS gate, see the comment above vbus_report. Reset bad so a returning cable
    # starts from a clean count.
    if [ "$(cat /sys/class/power_supply/usb/online 2>/dev/null)" != "1" ]; then
        bad=0
        continue
    fi

    # Heartbeat every ~5 min, so a quiet log is not mistaken for a dead watchdog.
    now=$(cut -d. -f1 /proc/uptime)
    if [ $((now - last_report)) -ge 300 ]; then
        log "heartbeat: udc=$u state='${st:-unknown}'$(vbus_report)"
        last_report=$now
    fi

    # Only "configured" is healthy. After a VBUS cut the state is "default"
    # (powered, never enumerated), so the intermediate states must count as bad.
    # That is safe with the 3-sample threshold: real enumeration walks
    # default -> addressed -> configured in milliseconds.
    case "$st" in
        configured)
            bad=0
            # A configured gadget does not mean adb works: the host can
            # enumerate the adb interface while adbd answers nothing. Restarting
            # adbd is cheap, so do it as soon as it is seen inactive.
            # Limit: an adbd that systemd considers active but that is wedged on
            # its FunctionFS endpoint is not caught here.
            if ! systemctl is-active --quiet android-tools-adbd; then
                log "gadget configured but adbd is $(systemctl is-active android-tools-adbd) -- restarting it"
                systemctl restart android-tools-adbd 2>/dev/null || log "  adbd restart failed"
            fi
            ;;
        *)
            # "not attached", empty, or anything unexpected.
            bad=$((bad + 1))
            # Log the raw state string, to check the state names if recovery
            # misbehaves.
            log "VBUS present but UDC state='${st:-unknown}' (${bad}/${NEED})"
            if [ "$bad" -ge "$NEED" ]; then
                attempts=$((attempts + 1))
                log "attempt #${attempts}:$(vbus_report)"
                rebind "$attempts"
                bad=0
                # Back off after 3 attempts, so a gadget that cannot recover
                # is not rebound every 45 s forever.
                if [ "$attempts" -ge 3 ]; then
                    log "3 attempts made; backing off to 5 min between tries"
                    sleep 300
                else
                    # Short gap, so the dwc3 re-init (the only level that
                    # fixes a VBUS cut) comes ~65 s after the cable returns.
                    sleep 15
                fi
            fi
            ;;
    esac
done
