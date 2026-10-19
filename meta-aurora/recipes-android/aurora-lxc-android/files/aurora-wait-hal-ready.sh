#!/bin/sh
# Drop /run/aurora-hal-ready once the Android container's HALs are actually
# binder-ready, so HAL clients (the launcher's libhybris compositor via the
# launcher precondition, and bluebinder) don't race HAL registration.
#
# The trap: init.svc.<hal>=running flips the moment Android init exec()s the HAL
# process -- up to ~a second before its binder service is registered with
# hwservicemanager and actually serviceable. Boot-to-boot jitter in that gap
# means clients that connect on the property edge (bluebinder_wait.sh polls
# exactly that property; lipstick's libhybris getService()s the composer) win
# the race ~half the time and crash/stall the HAL the other half -- the bimodal
# fast/slow boot. lshal only lists a HAL once it is registered with
# hwservicemanager, so it is the real "serviceable now" signal.
#
# This barrier also restores the settle time the old design got for free by
# starting the compositor late: we hold the UI until the HALs are up (or a
# deadline), instead of letting it race a half-initialised container.
FLAG=/run/aurora-hal-ready
DEADLINE=$(( $(date +%s) + 35 ))   # hard wall-clock cap; never block the UI forever

hal_ready() {
    pid=$(lxc-info -n android -p 2>/dev/null | awk '{print $2}')
    [ -n "$pid" ] || return 1
    # lshal can BLOCK on an unresponsive HAL, so bound each probe with timeout;
    # and it exits non-zero (72) even on success, so gate on the greps, not $?.
    hal=$(timeout 5 nsenter -t "$pid" -m -p -- /system/bin/lshal 2>/dev/null)
    echo "$hal" | grep -q 'IComposer/default'     || return 1
    echo "$hal" | grep -q 'IBluetoothHci/default' || return 1
    return 0
}

while [ "$(date +%s)" -lt "$DEADLINE" ]; do
    if hal_ready; then
        echo "aurora-hal-ready: composer + bluetooth HALs registered"
        : > "$FLAG"
        exit 0
    fi
    sleep 1
done

echo "aurora-hal-ready: deadline reached before HALs registered; releasing UI anyway" >&2
: > "$FLAG"
exit 0
