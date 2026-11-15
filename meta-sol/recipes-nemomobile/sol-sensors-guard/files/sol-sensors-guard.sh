#!/bin/sh
# /usr/bin/sol-sensors-guard.sh
#
# Notice when sensorfw's D-Bus interface has gone deaf, and put it back. Does
# nothing on a healthy watch. Stop sol-sensors-guard.timer before debugging
# sensors or the container by hand, or this restarts the container under you.
#
# After an abnormal container restart the HIDL sensors HAL (sol-sensors-hidl)
# can end up unregistered on hwbinder. sensorfw's HybrisManager::finishConnect()
# then sleeps in a loop on its main thread and answers nothing on D-Bus. mce
# retries loadPlugin every 10 s, hits the 128 pending-reply cap, and its
# connection jams: the power key no longer wakes the screen.
#
# A healthy sensorfw answers Introspect at once; three misses over ~20 s mean a
# wedge. Kicks, cheapest first:
#   1. restart sensorfwd: fixes a transient fault only. A fresh sensorfwd looks
#      healthy until mce's next retry, so recovery is confirmed over ~13 s.
#   2. restart sol-lxc-android: re-applies the HIDL bridge in order so the HAL
#      registers. Visible, so deferred while the display is on and rate-limited.
#
# The timer is frozen while the watch is suspended, so it wakes nothing extra.
set -u

DEST=com.nokia.SensorService
OBJ=/SensorManager
STAMP=/run/sol-sensors-guard.last          # uptime-seconds of the last container kick
BACKOFF=1800                               # >= 30 min between container restarts

log() { echo "sol-sensors-guard: $*"; }

# sensorfw D-Bus alive?  0 = healthy (replied), non-zero = wedged/absent.
probe() {
    timeout 8 dbus-send --system --print-reply --reply-timeout=6000 \
        --dest="$DEST" "$OBJ" \
        org.freedesktop.DBus.Introspectable.Introspect >/dev/null 2>&1
}

# Recovery must hold across one mce retry period (~10 s): two passes 13 s apart.
confirm_healthy() {
    probe || return 1
    sleep 13
    probe || return 1
    return 0
}

display_state() {
    timeout 10 mcetool 2>/dev/null | sed -n 's/^Display state:[[:space:]]*//p'
}

# Only while the container is up; during a (re)start silence is expected.
[ "$(systemctl is-active sol-lxc-android 2>/dev/null)" = active ] || exit 0

# ─── Is anything wrong? Confirm a PERSISTENT wedge, 3 misses over ~20 s ───────
i=1
while [ "$i" -le 3 ]; do
    if probe; then exit 0; fi              # any reply => healthy, done
    [ "$i" -lt 3 ] && sleep 8
    i=$((i + 1))
done
log "sensorfw D-Bus unresponsive on 3/3 Introspect probes -- wedged (likely an unregistered HIDL sensors HAL)"

# ─── Kick 1: cheap and invisible, restart sensorfwd ───
systemctl restart sensorfwd >/dev/null 2>&1
if confirm_healthy; then log "recovered after restarting sensorfwd"; exit 0; fi

# ─── Kick 2: the real fix, but visible: defer while the display is on ───
case "$(display_state)" in
    off|lpm_on|lpm_off) ;;                  # screen not in use -- safe to blink the container
    *)
        log "still wedged, but the display is on (wearer looking) -- deferring the container restart to a later cycle"
        exit 0 ;;
esac

# Rate-limit: never restart the container more than once per BACKOFF.
now=$(cut -d. -f1 /proc/uptime 2>/dev/null || echo 0)   # monotonic; sol's wall clock is unreliable
last=0; [ -r "$STAMP" ] && last=$(cat "$STAMP" 2>/dev/null || echo 0)
if [ "$last" != 0 ] && [ $((now - last)) -lt "$BACKOFF" ]; then
    log "still wedged, but the container was restarted $((now - last))s ago (< ${BACKOFF}s) -- backing off"
    exit 0
fi
echo "$now" > "$STAMP"

log "restarting the Android container to re-register the HIDL sensors HAL"
systemctl restart sol-lxc-android >/dev/null 2>&1

# wait (up to 2 min) for the sensors HAL process to reappear
i=0
while [ "$i" -lt 24 ]; do
    sleep 5; i=$((i + 1))
    pgrep -f sensors-service.multihal >/dev/null 2>&1 && break
done
sleep 5
systemctl restart sensorfwd >/dev/null 2>&1     # clean connect to the now-registered HAL
sleep 8
systemctl restart mce >/dev/null 2>&1           # drain any pending-reply saturation mce built up
# The launcher keeps its hwc2 connection to the old container's composer: the
# panel stays off and the face frozen until it reconnects.
systemctl --user -M ceres@ restart asteroid-launcher >/dev/null 2>&1

if confirm_healthy; then
    log "recovered after re-running the container bring-up (+ sensorfwd + mce)"
else
    log "STILL wedged after a container restart -- needs a look (HIDL sensors HAL not registering?)"
fi
