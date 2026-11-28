#!/bin/sh
# /usr/bin/sol-sleep-guard.sh
#
# Notice when the watch has silently stopped being able to sleep, and nudge it
# back. Does nothing on a healthy watch.
#
# Failure 1: mce holds its mce_display_on wakelock with the display off and
# leaves /sys/power/autosleep "off", so suspend is never attempted (~72 mA).
# Test: autosleep off, display off, mce_display_on held, and suspend_stats
# success and fail both frozen. A rising fail count means suspend is attempted
# and aborted by a wakeup source, which kicking mce would not fix.
#
# Kicks, cheapest first: re-blank; toggle low-power mode (this clears it on
# sol, where a restart does not); restart mce (clears it on aurora).
#
# The timer is frozen while the watch is asleep, so on a healthy watch this
# adds no wakeups.
set -u

AUTOSLEEP=/sys/power/autosleep
WAKELOCK=/sys/power/wake_lock
OK=/sys/power/suspend_stats/success
FAIL=/sys/power/suspend_stats/fail
STAMP=/run/sol-sleep-guard.last

log() { echo "sol-sleep-guard: $*"; }

[ -r "$AUTOSLEEP" ] || exit 0

display_state() {
    timeout 10 mcetool 2>/dev/null | sed -n 's/^Display state:[[:space:]]*//p'
}

# ─── Failure 2: a leaked mce_mux ───────────────────────────────────────────
# mce folds its internal locks, including one per pending D-Bus call, into one
# kernel wakelock, mce_mux. Leaked call locks can hold it from boot with
# autosleep still "mem" and success=0 fail=0, draining the battery in ~14 h;
# `systemctl restart mce` releases them. Normal holds are under ~30 s. Held for
# 5 min, display off, no suspend since the last look: restart mce and re-blank
# (a restart turns the display on). At most once an hour, logged to
# /var/lib/sol-sleep-guard.log as evidence for an mce report.
MUX_STAMP=/run/sol-sleep-guard.mux
MUX_LOG=/var/lib/sol-sleep-guard.log
mux_held_ms=$(awk '$1=="mce_mux"{print $6}' /sys/kernel/debug/wakeup_sources 2>/dev/null)
if [ "${mux_held_ms:-0}" -gt 300000 ]; then
    case "$(display_state)" in
        off|lpm_on|lpm_off)
            now_ok=$(cat "$OK" 2>/dev/null || echo 0)
            now_s=$(cut -d. -f1 /proc/uptime)
            last_s=-3600; last_ok=-1
            [ -r "$MUX_STAMP" ] && read -r last_s last_ok < "$MUX_STAMP"
            if [ "$now_ok" = "$last_ok" ] && [ $((now_s - last_s)) -ge 3600 ]; then
                msg="mce_mux held ${mux_held_ms} ms, display off, no suspend since last look (ok=$now_ok) -- restarting mce; wake_lock=[$(cat "$WAKELOCK")]"
                log "$msg"
                echo "$(date -u +%FT%TZ) up=${now_s}s $msg" >> "$MUX_LOG"
                systemctl restart mce >/dev/null 2>&1
                sleep 8
                timeout 10 mcetool --blank-screen >/dev/null 2>&1
                echo "$now_s $now_ok" > "$MUX_STAMP"
                exit 0
            fi
            [ "$now_ok" = "$last_ok" ] || echo "${last_s:--3600} $now_ok" > "$MUX_STAMP"
            ;;
    esac
fi

# ─── Is anything wrong? ──────────────────────────────────────────────────────
[ "$(cat "$AUTOSLEEP" 2>/dev/null)" = "off" ] || exit 0

case "$(display_state)" in
    off|lpm_on|lpm_off) ;;                 # screen not in use -- should be sleeping
    *) exit 0 ;;                           # user is looking at it; awake is correct
esac

grep -q mce_display_on "$WAKELOCK" 2>/dev/null || exit 0

# Distinguish "never arms" from "arms and aborts". Only the former is ours.
now_ok=$(cat "$OK" 2>/dev/null || echo 0)
now_fail=$(cat "$FAIL" 2>/dev/null || echo 0)
if [ -r "$STAMP" ]; then
    read -r prev_ok prev_fail < "$STAMP" 2>/dev/null || { prev_ok=""; prev_fail=""; }
    if [ "${prev_fail:-}" != "" ] && [ "$now_fail" -gt "${prev_fail:-0}" ]; then
        log "suspend is being ATTEMPTED and aborted (fail $prev_fail -> $now_fail) -- a wakeup source is to blame, not mce; leaving alone"
        echo "$now_ok $now_fail" > "$STAMP"
        exit 0
    fi
    if [ "${prev_ok:-}" != "" ] && [ "$now_ok" -gt "${prev_ok:-0}" ]; then
        echo "$now_ok $now_fail" > "$STAMP"          # it did sleep since last look
        exit 0
    fi
fi
echo "$now_ok $now_fail" > "$STAMP"

log "stuck: autosleep=off, display off, mce_display_on held, suspend never attempted (ok=$now_ok fail=$now_fail)"

armed() { [ "$(cat "$AUTOSLEEP" 2>/dev/null)" = "mem" ]; }

# ─── Kick, cheapest first ────────────────────────────────────────────────────
timeout 10 mcetool --blank-screen >/dev/null 2>&1
sleep 3
if armed; then log "recovered after re-blanking"; exit 0; fi

# Toggle, then restore the wearer's setting so AoD is not forced on.
lpm_was=$(timeout 10 mcetool 2>/dev/null | sed -n 's/^Use low power mode:[[:space:]]*//p')
timeout 10 mcetool --set-low-power-mode=disabled >/dev/null 2>&1
sleep 2
timeout 10 mcetool --set-low-power-mode=enabled  >/dev/null 2>&1
[ "$lpm_was" = "disabled" ] && { sleep 2; timeout 10 mcetool --set-low-power-mode=disabled >/dev/null 2>&1; }
sleep 3
if armed; then log "recovered after a low-power-mode toggle (setting restored: ${lpm_was:-?})"; exit 0; fi

log "still stuck -- restarting mce"
systemctl restart mce >/dev/null 2>&1
sleep 8
timeout 10 mcetool --blank-screen >/dev/null 2>&1
sleep 3
if armed; then log "recovered after restarting mce"; else log "STILL STUCK after all three kicks -- needs a look"; fi
