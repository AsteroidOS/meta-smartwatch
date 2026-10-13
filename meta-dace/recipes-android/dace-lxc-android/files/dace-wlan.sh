#!/bin/sh
# dace: WLAN (qcacld/icnss2) bring-up, aurora-style.
#
# WHAT IT DOES (and why in this order):
#   1) ADSP up. The modem firmware (WPSS) and the WLAN one wait for the ADSP to
#      be alive (via tmr_slave2). dace-vendor-mount already starts it; here it is
#      only checked.
#   2) WLAN chain from the rootfs (qcacld + cnss glue). dace-modules-load.service
#      already loads it; here it is re-forced idempotently in case the order or a
#      one-off failure left it half done. The `wlan` .ko is the qcacld driver and
#      `icnss2` the integrated subsystem that bridges QMI (WLFW) with the modem
#      firmware.
#   3) Wait for the slate MCU. On dace the icnss node carries `qcom,is_slate_rfa`
#      (the WLAN RFA lives in the slate MCU): icnss2 BLOCKS its connectivity
#      until the "slatefw" AFTER_POWERUP SSR. The MCU is started by
#      dace-slate-mcu.service (late, ~60 s); here we wait.
#   4) cnss-daemon inside the container. It needs /data/vendor/wifi/sockets
#      (created by dace-lxc-android-start.sh BEFORE launching the Android init,
#      otherwise `Fail to bind user socket`). If it has not started, it is
#      launched with ctl.start (with a timeout: lxc-attach hangs from a unit).
#   5) Start the modem (remoteproc1). The WLFW service (QMI 0x45) that icnss2
#      waits for is served by the modem WPSS firmware; without the modem there is
#      no `wlan0`. GATED behind /etc/dace-kick-modem: today the modem firmware
#      dies with `DOG detects stalled initialization` at ~40 s, and with
#      recovery=enabled it does NOT take down the watch, but it does not give
#      WLAN either. Once the modem block is solved, creating that file is
#      enough.
#   6) Wait for `wlan0` and dump diagnostics to /var/log/dace-wlan.log.
#
# AURORA: its aurora-vendor-mount.sh starts the modem without a gate (and lives
# with its crashes: "modem perpetually crashes & recovers"). We keep it gated so
# as not to put a crash-loop into a watch that is stable today, but the chain
# (modules + ADSP + cnss + firmware_mnt) is exactly its own.
set -u

LOG=/var/log/dace-wlan.log
log() {
    _l="$(date '+%H:%M:%S') dace-wlan: $*"
    echo "$_l" >> /run/dace-wlan.log
    echo "$_l" >> "$LOG" 2>/dev/null
    sync 2>/dev/null
}

# Do NOT write to journald/console (StandardOutput=null): with journald stuck the
# write BLOCKS and the unit dies by timeout (same lesson as dace-slate-mcu).

log "=== WLAN bring-up ==="

# 1) ADSP -------------------------------------------------------------------
i=0
while [ $i -lt 60 ]; do
    [ "$(cat /sys/class/remoteproc/remoteproc0/state 2>/dev/null)" = "running" ] && break
    i=$((i + 1)); sleep 1
done
log "adsp=$(cat /sys/class/remoteproc/remoteproc0/state 2>/dev/null)"

# 2) WLAN chain -------------------------------------------------------------
# google_wlan_mac reads the MAC from the DT (/chosen/config, which the Mobvoi ABL
# does not create) and can fail: it is harmless, qcacld falls back to the MAC from
# WCNSS_qcom_cfg.ini.
for _m in cfg80211 cnss_utils google_wlan_mac cnss_prealloc cnss_nl \
          wlan_firmware_service cnss_plat_ipc_qmi_svc icnss2 wlan; do
    modprobe "$_m" 2>/dev/null
done
_n=$(lsmod 2>/dev/null | grep -cE '^(wlan|icnss2|cnss_utils|cnss_prealloc|cnss_nl|wlan_firmware_service|cnss_plat_ipc_qmi_svc) ')
log "WLAN chain loaded ($_n modules)"

# 3) slate MCU --------------------------------------------------------------
i=0
while [ $i -lt 300 ]; do
    [ "$(cat /sys/kernel/slate_bt_state/slate_bt_state 2>/dev/null)" = "ready" ] && break
    i=$((i + 1)); sleep 1
done
log "slate_bt_state=$(cat /sys/kernel/slate_bt_state/slate_bt_state 2>/dev/null) (ready expected)"

# 4) container services that stock starts and ours does NOT -------------------
#  - cnss-daemon: WLAN cnss-genl bridge; it needs /data/vendor/wifi/sockets
#    (created by dace-lxc-android-start.sh BEFORE the Android init).
#  - vendor.pd_mapper: "servloc"/Servreg (PD mapper) server. It parses the
#    firmware *.jsn files (including modemr/modemuw.jsn) and serves the
#    Protection Domain mapping over QMI. Our container init does NOT start the
#    `class core` services from init.target.rc (getprop init.svc.vendor.
#    pd_mapper = empty): without it there is no service locator for the modem/ADSP.
#  - vendor.per_mgr: stock peripheral manager (pm-service).
# real command each service runs (== /proc/<pid>/comm)
#   cnss-daemon        -> cnss-daemon
#   vendor.pd_mapper   -> pd-mapper
#   vendor.per_mgr     -> pm-service
procs_of() {
    case "$1" in
        cnss-daemon)      echo cnss-daemon ;;
        vendor.pd_mapper) echo pd-mapper ;;
        vendor.per_mgr)   echo pm-service ;;
        *)                echo "$1" ;;
    esac
}
proc_running() {
    _comm=$(procs_of "$1")
    for _p in /proc/[0-9]*/comm; do
        [ "$(cat "$_p" 2>/dev/null)" = "$_comm" ] && return 0
    done
    return 1
}
_started=""
for _s in cnss-daemon vendor.pd_mapper vendor.per_mgr; do
    if proc_running "$_s"; then
        _started="$_started ${_s}(already)"
    else
        timeout 15 lxc-attach -n android -- /system/bin/setprop ctl.start "$_s" 2>/dev/null
        _started="$_started ${_s}"
    fi
done
sleep 3
log "container services:$_started"
for _s in cnss-daemon vendor.pd_mapper vendor.per_mgr; do
    if proc_running "$_s"; then
        log "  $_s running ($(procs_of "$_s"))"
    else
        log "  WARNING: $_s NOT running ($(procs_of "$_s"))"
    fi
done

# 5) start the modem (gate) ------------------------------------------------
if [ -e /etc/dace-kick-modem ]; then
    if [ "$(cat /sys/class/remoteproc/remoteproc1/state 2>/dev/null)" = "offline" ]; then
        log "starting the modem (remoteproc1)..."
        echo start > /sys/class/remoteproc/remoteproc1/state 2>/dev/null
    fi
    log "mss=$(cat /sys/class/remoteproc/remoteproc1/state 2>/dev/null)"
else
    log "modem NOT started (create /etc/dace-kick-modem to attempt WLAN)"
fi

# 6) wait for wlan0 ---------------------------------------------------------
i=0
while [ $i -lt 75 ]; do
    if ip -o link 2>/dev/null | grep -q "wlan0"; then
        log "wlan0 UP: $(ip -o link show wlan0 2>/dev/null)"
        break
    fi
    i=$((i + 1)); sleep 2
done
[ $i -ge 75 ] && log "WARNING: wlan0 does not appear"

# Diagnostics for the next session (can be read without adb).
{
    echo "=== wlan report $(date) ==="
    echo "-- interfaces --"; ip -o link 2>/dev/null | grep -iE "wlan|wifi" || echo "(no wlan)"
    echo "-- rprocs --"; for r in /sys/class/remoteproc/remoteproc*; do
        echo "  $(basename $r)=$(cat $r/state 2>/dev/null)"; done
    echo "-- slate --"; cat /sys/kernel/slate_bt_state/slate_bt_state 2>/dev/null; echo
    echo "-- cnss-daemon --"; grep -l cnss-daemon /proc/[0-9]*/comm 2>/dev/null
    echo "-- modem QRTR services (0x45=WLFW) --"
    grep -aoE "service_announce_new: \[0x[0-9a-f]+:0x[0-9a-f]+\]@\[0x[0-9a-f]+:0x[0-9a-f]+\]" \
        /sys/kernel/debug/ipc_logging/qrtr_ns/log 2>/dev/null | sort -u
    echo "-- icnss (WLFW/MSA) --"
    grep -iE "WLFW|server arrive|MSA|FW is ready|FW Initialization" \
        /sys/kernel/debug/ipc_logging/icnss/log 2>/dev/null | tail -15
} >> "$LOG" 2>/dev/null
sync
exit 0
