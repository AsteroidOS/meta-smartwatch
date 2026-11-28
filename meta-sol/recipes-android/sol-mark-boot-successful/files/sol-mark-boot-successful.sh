#!/bin/sh
# /usr/bin/sol-mark-boot-successful.sh
#
# Mark the current A/B slot successful. The bootloader decrements the slot's
# retry count on every boot; at zero it marks the slot unbootable and drops to
# fastboot, and recovery needs a host.
#
# AOSP runs update_verifier from init.rc on sys.boot_completed=1. AsteroidOS
# does not run the framework, so nothing sets that property and the binary in
# the container image never runs. Running it directly is enough.
#
# Never fatal: a watch that cannot mark its slot should still boot.
set -u

# Wait for the container and its boot-control HAL. sol-lxc-android.service is
# active before the container has a PID, and update_verifier run before the
# HAL is up exits 0 and marks nothing. The HAL comes up about 25 s in.
PID=
for _ in $(seq 1 60); do
    PID=$(lxc-info -n android -p -H 2>/dev/null)
    [ -n "$PID" ] && pgrep -f android.hardware.boot-service >/dev/null && break
    PID=
    sleep 3
done
[ -n "$PID" ] || {
    echo "sol-mark-boot-successful: container or boot-control HAL not up after 180 s -- slot NOT marked"
    exit 0
}

if ! nsenter -t "$PID" -m -p -- /system/bin/ls /system/bin/update_verifier >/dev/null 2>&1; then
    echo "sol-mark-boot-successful: no update_verifier in the container image -- skipping"
    exit 0
fi
sleep 5

nsenter -t "$PID" -m -p -- /system/bin/update_verifier >/dev/null 2>&1
echo "sol-mark-boot-successful: ran update_verifier; slot should now be marked successful"
exit 0
