#!/bin/sh
# Bring up the modem subsystem, which hosts sol's GNSS engine.
#
# Nothing binds qcom_q6v5_pas to 6080000.remoteproc-mss at boot, so geoclue's
# provider starts and then waits forever. This binds and starts it.
#
# Firmware path: the modem's firmware is in a different directory from the
# ADSP's:
#
#     ADSP   /android/vendor/firmware/adsp.mdt
#     modem  /android/vendor/firmware_mnt/image/modem.mdt
#
# firmware_class.path holds one value, which sol-adsp-boot has set to the ADSP
# directory. Without modem.mdt in reach the start fails with ENOENT. So point
# it at the modem directory, start, and restore it: the loader reads the
# parameter only during a load, and leaving it on the modem directory breaks
# the next firmware load.
#
# Ships disabled. A live positioning session used to reset the SoC within
# 40-100 s. That no longer reproduces, but the change that fixed it was never
# isolated, so enabling it is left as a deliberate decision.
set -u

RPROC=/sys/class/remoteproc/remoteproc1
DRV=/sys/bus/platform/drivers/qcom_q6v5_pas
FWPATH=/sys/module/firmware_class/parameters/path
MODEM_FW=/android/vendor/firmware_mnt/image
ADSP_FW=/android/vendor/firmware

[ -f "$MODEM_FW/modem.mdt" ] || {
    echo "sol-modem-boot: $MODEM_FW/modem.mdt missing -- is sol-vendor-mount up?"
    exit 0
}

if [ ! -d "$RPROC" ]; then
    echo "sol-modem-boot: binding 6080000.remoteproc-mss"
    echo 6080000.remoteproc-mss > "$DRV/bind" 2>/dev/null || {
        echo "sol-modem-boot: bind failed"; exit 0; }
    sleep 3
fi

[ -d "$RPROC" ] || { echo "sol-modem-boot: $RPROC absent after bind"; exit 0; }

name=$(cat "$RPROC/name" 2>/dev/null)
case "$name" in
    *remoteproc-mss) : ;;
    *) echo "sol-modem-boot: remoteproc1 is '$name', not the modem -- refusing"; exit 0 ;;
esac

state=$(cat "$RPROC/state" 2>/dev/null)
[ "$state" = "running" ] && { echo "sol-modem-boot: already running"; exit 0; }

echo "$MODEM_FW" > "$FWPATH" 2>/dev/null || echo "sol-modem-boot: could not set $FWPATH"
echo start > "$RPROC/state" 2>/dev/null || echo "sol-modem-boot: start failed (firmware not found?)"
sleep 5
echo "$ADSP_FW" > "$FWPATH" 2>/dev/null || echo "sol-modem-boot: could not restore $FWPATH"

# Report the value written: the parameter is mode 0200, so a read-back always
# prints an empty string.
echo "sol-modem-boot: remoteproc1 state=$(cat "$RPROC/state" 2>/dev/null), fw path restored to $ADSP_FW"

# Restart the GNSS HAL. The container starts it at boot, before the modem runs;
# a HAL that initialises without a modem never gets a QMI client, does not
# retry, and logs nothing (not even with IZAT_DEBUG_LEVEL=2). Restarted after
# the modem is up, it connects at once.
[ "$(cat "$RPROC/state" 2>/dev/null)" = "running" ] || exit 0

restart_gnss_hal() {
    # A restart request that lands before init has started the service is
    # silently lost, so wait for it to exist first.
    i=0
    while [ "$i" -lt 60 ]; do
        ps -eo args 2>/dev/null | grep -q "[g]nss-aidl-service-qti" && break
        sleep 1
        i=$((i + 1))
    done
    echo "sol-modem-boot: gnss HAL present after ${i}s"

    # Prefer ctl.restart: a killed HAL never closes its QMI session, so the
    # modem keeps the old owner and the next client gets
    # QMI_ERR_SESSION_OWNERSHIP_V01. The kill is a fallback only.
    if [ -x /system/bin/setprop ] && /system/bin/setprop ctl.restart gnss_service 2>/dev/null; then
        echo "sol-modem-boot: requested ctl.restart gnss_service"
    else
        pid=$(ps -eo pid,args 2>/dev/null | grep "[g]nss-aidl-service-qti" | head -n 1 | awk '{print $1}')
        if [ -n "$pid" ]; then
            kill -9 "$pid" 2>/dev/null && echo "sol-modem-boot: killed gnss HAL pid $pid, init will respawn it"
        else
            echo "sol-modem-boot: gnss HAL not running, nothing to restart"
            return
        fi
    fi
    # Give init time to bring it back before anything asks for a position.
    i=0
    while [ "$i" -lt 30 ]; do
        sleep 1
        ps -eo args 2>/dev/null | grep -q "[g]nss-aidl-service-qti" && break
        i=$((i + 1))
    done
    echo "sol-modem-boot: gnss HAL back after ${i}s"
}

restart_gnss_hal
