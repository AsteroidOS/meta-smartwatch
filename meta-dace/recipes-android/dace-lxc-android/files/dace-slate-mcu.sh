#!/bin/sh
# dace: starts the slate MCU (remoteproc2) for BLUETOOTH.
#
# On the watch, persist.vendor.qcom.bluetooth.soc=slate: the BT HAL does not
# talk to the chip over the AP UART, but over the MCT transport through the
# MCU's glink link, and the MCU is what powers/clocks the chip. Without the MCU
# the chip is MUTE (Get Version never answers).
#
# ORDER AND TIMING MATTER (measured 2026-09-20):
#   1) the slate .ko's are already loaded (udev) when this runs;
#   1b) `powerstateservice-hal-1-0` (pss) must be STARTED: it is the MCU's state
#      peer. Its .rc does NOT have an 'interface' line -> ctl.start.
#      Without it, the MCU is left with nobody to answer it and the SoC ends in
#      an Oops (jump to address 0) a few seconds later;
#   2) on `echo start` in the rproc, the subdev SSR of qcom_rproc_slate
#      notifies "slatefw" QCOM_SSR_AFTER_POWERUP -> slatecom_set_spi_state(
#      SLATECOM_SPI_FREE) requests the "qcom-slate_spi" IRQ and its tasklet reads
#      the MCU status registers over SPI (the path that the SMMU aborted) ->
#      brings up the glink link and publishes slate_bt_state=ready;
#   3) the MCU is NOT started early in the boot: starting it at ~23 s
#      (while the container brings up its HALs) ended in that same Oops.
#      Started with the system already up (unit Ordered after graphical.target
#      + margin) it is stable and that is when the HAL makes use of it.
#
# CROWN/RSB (2026-09-22): the slate_rsb + slatersb_rpmsg modules are loaded
# HERE, before the `echo start`. Short reason: the slate-rsb-ctl channel does
# not exist until the MCU starts; if slatersb_rpmsg probes with slate_rsb absent
# (rsb_ops=NULL) it is a call to NULL -> oops -> panic (panic_on_oops=1) ->
# reset (documented crash). With the MCU stopped the order is
# the only safe one, and it is the same as stock (loads both in
# vendor_dlkm/modules.load before starting the MCU).
#
# v2 (2026-09-22): the RSB is NOT pre-enabled. With pending_enable=1 the driver,
# on completing the AFTER_POWERUP CONFIGR_RSB, sends SLATERSB_ENABLE to the MCU
# immediately -> prime suspect for the reset a few seconds after starting the
# MCU. ENABLE is done AT THE END, with BT already confirmed.
set -u

# The log goes to /run (tmpfs, live read) AND to /var/log (eMMC: survives a hard
# reset, which is exactly what needs debugging). sync per line: there are ~40.
log() {
    _l="$(date '+%H:%M:%S') dace-slate-mcu: $*"
    echo "$_l" >> /run/dace-slate-mcu.log
    echo "$_l" >> /var/log/dace-slate-mcu.log 2>/dev/null
    sync 2>/dev/null
}

# CRITICAL NOTE (2026-09-20): this service does NOT write to journald or the
# console (StandardOutput=null in the unit). Measured: with journald/logd stuck
# (it happens on loaded boots) any write to journald BLOCKS, and the script hung
# on its first message -> 'start operation timed out' and the MCU never started.
# The log goes to /run (tmpfs, no wear) and can be read later:
#   cat /run/dace-slate-mcu.log
# For the same reason lxc-attach is no longer used on the critical path (it
# hangs when launched by a unit): pss starts via the .rc 'interface' line and
# the HAL/bluebinder are relaunched by killing them via /proc.

# 2) powerstateservice (vendor.qti.hardware.powerstateservice@1.0).
#    It is the slate MCU's state PEER (TWM/deep-sleep): /dev/power_state and
#    /dev/slate_com_dev. It is started by the container init THANKS to the
#    'interface' line that dace-lxc-android-start.sh adds to its .rc (its
#    original .rc does not have it and that is why it never started: 'Could not
#    find ...IPowerStateService/default' every 60 s).
#    IT MUST BE STARTED BEFORE BRINGING UP THE MCU: without it, the MCU is left
#    with nobody to answer its state changes and the SoC ends in an Oops.
#    lxc-attach IS NOT USED HERE (it hangs from a unit): we only check that the
#    service opened /dev/power_state.
for _i in 1 2 3 4 5 6; do
    [ -c /dev/power_state ] && break
    sleep 2
done
if [ -c /dev/power_state ]; then
    log "pss OK (/dev/power_state present)"
else
    log "WARNING: /dev/power_state absent: pss did not start (check the .rc/overlay)"
fi

# 3) Wait for the MCU remoteproc to exist (qcom_rproc_slate creates it at probe;
#    its .ko is also requested by udev, but it can be late).
i=0
while [ $i -lt 20 ]; do
    [ -e /sys/class/remoteproc/remoteproc2/state ] && break
    i=$((i + 1))
    sleep 1
done
if [ ! -e /sys/class/remoteproc/remoteproc2/state ]; then
    log "WARNING: /sys/class/remoteproc/remoteproc2/state does not exist (slate stack not loaded)"
    exit 0
fi
RSB=/sys/devices/platform/soc/soc:qcom,slate-rsb/enable

# (crown) Load the RSB stack now — ALWAYS with the MCU stopped (the `echo
# start` is further below): slatersb_rpmsg probing with slate_rsb absent
# (rsb_ops.glink_channel_state=NULL) is a call to NULL -> oops -> reset.
# Fallback in case dace-modules-load failed or is masked (boot noautoload); on a
# normal boot they already come from there and this does nothing.
if ! grep -q '^slate_rsb ' /proc/modules; then
    timeout 8 modprobe slatersb_rpmsg slate_rsb >> /run/dace-slate-mcu.log 2>&1
fi
if grep -q '^slate_rsb ' /proc/modules; then
    log "crown: slate_rsb+slatersb_rpmsg loaded (enable=$(if [ -e "$RSB" ]; then echo yes; else echo no; fi))"
else
    # Without slate_rsb, opening the channel would crash: unload slatersb (it
    # never probed, the channel does not exist yet) and the blacklist prevents
    # udev from reloading it on its own -> crown inert, but BT starts and the
    # watch does NOT reset.
    log "WARNING: crown: slate_rsb did NOT load; unloading slatersb_rpmsg if it is loose"
    rmmod slatersb_rpmsg >> /run/dace-slate-mcu.log 2>&1
fi

# (crown/safety) qcom_rproc_slate hardcodes recovery_disabled=true and
# slate_restart_work does BUG_ON(recovery_disabled): an MCU crash = BUG = kernel
# panic = reset. With 'enabled' (same trick as the modem DOG in
# dace-vendor-mount) that crash goes to rproc recovery. No-op if it is not crashed.
if echo enabled > /sys/class/remoteproc/remoteproc2/recovery 2>/dev/null; then
    log "crown/safety: remoteproc2 recovery=enabled"
fi

case "$(cat /sys/class/remoteproc/remoteproc2/state 2>/dev/null)" in
    running)
        log "the MCU was already started"
        ;;
    *)
        # CROWN v2: do NOT pre-enable the RSB here (see header). The enable goes
        # at the end, with BT already up.
        # 5) Start the MCU -> SSR notification -> SPI FREE + glink -> BT ready
        log "starting the MCU (remoteproc2)..."
        if echo start > /sys/class/remoteproc/remoteproc2/state 2>/dev/null; then
            log "MCU state=$(cat /sys/class/remoteproc/remoteproc2/state 2>/dev/null)"
        else
            log "WARNING: MCU start failed (firmware? see dace-vendor-mount)"
        fi
        ;;
esac

# 6) Wait (max ~10 s) for the link to become usable: BT ready and RSB channel.
i=0
while [ $i -lt 20 ]; do
    bt="$(cat /sys/kernel/slate_bt_state/slate_bt_state 2>/dev/null)"
    [ "$bt" = "ready" ] && break
    i=$((i + 1))
    sleep 1
done
log "slate_bt_state=${bt:-?} dsp_state=$(cat /sys/kernel/slate_dsp_state/slate_dsp_state 2>/dev/null)"
if [ "$(cat /sys/kernel/slate_bt_state/slate_bt_state 2>/dev/null)" = "ready" ]; then
    # The BT HAL gives up after ~3 attempts (one per minute) if BTSS was not
    # ready yet, and does NOT retry on its own (the process stays alive but
    # idle). With the MCU already up it must be relaunched: this is exactly the
    # sequence verified live (full bring-up -> hci0 UP RUNNING with its BD
    # Address).
    # Power (cycle) the BT chip: with soc=slate the HAL does not vote
    # regulators and the pm5100_l13/l17 rails stay OFF -> mute chip.
    # This is what aurora does via /dev/btpower. The 0->1 cycle also resets the
    # chip, which restarts at 2400 bps (the state the HAL expects).
    if [ -x /usr/libexec/dace-bt-power.pl ]; then
        if /usr/bin/perl /usr/libexec/dace-bt-power.pl cycle >> /run/dace-slate-mcu.log 2>&1; then
            log "BT chip powered (BT_CMD_PWR_CTRL cycle)"
        else
            log "WARNING: BT chip power failed (/dev/btpower)"
        fi
    fi
    # BT HAL and its CLIENT, relaunched WITHOUT lxc-attach (measured 2026-09-20:
    # `lxc-attach` hangs when launched by a systemd unit with the system loaded
    # -- dbus/systemd saturated: it works from the shell, not from a service).
    # Instead we kill them via /proc: the container init relaunches the HAL
    # (verified: the pid changes) and systemd relaunches bluebinder
    # (Restart=always). Without a FRESH bluebinder the HAL never gets to
    # initialize ("Waiting for bluetooth service" forever) and hci0 stays
    # without a BD address. With both fresh + slate ready -> hci0 UP RUNNING.
    for _p in /proc/[0-9]*; do
        _c=$(cat "$_p/comm" 2>/dev/null)
        case "$_c" in
            *bluetooth@1.0*) kill -9 "${_p#/proc/}" 2>/dev/null && log "BT HAL relaunched (kill ${_p#/proc/})" ;;
            bluebinder)      kill -9 "${_p#/proc/}" 2>/dev/null && log "bluebinder relaunched (kill ${_p#/proc/})" ;;
        esac
    done
    log "slate OK (BT ready)"
else
    log "WARNING: slate_bt_state is not ready (check the glink link)"
fi

# 7) Wait for the controller to become operational (hci0 with a BD address).
# NOTE: the REAL bring-up takes ~100 s from here: the HAL is relaunched but its
# first attempt with the chip already powered comes on the next cycle (~60 s) and
# the patch+NVM download takes a few more seconds (measured 2026-09-21: hci0
# comes up at ~90-110 s). The previous cap (40 x 2 s = 80 s) was too short and
# the log spat out a misleading WARNING even though everything ended fine: hence
# 150 x 2 s = 300 s.
i=0
while [ $i -lt 150 ]; do
    if hciconfig 2>/dev/null | grep -qE "BD Address: ([0-9A-Fa-f]{2}:){5}" && \
       ! hciconfig 2>/dev/null | grep -q "BD Address: 00:00:00:00:00:00"; then
        log "hci0 READY: $(hciconfig 2>/dev/null | sed -n 2p | tr -s " ")"
        break
    fi
    i=$((i + 1))
    sleep 2
done
[ $i -ge 150 ] && log "WARNING: hci0 did not come up in ~300 s (check the container logcat)"

# 8) CROWN v2: enable the RSB AT THE END. The CONFIGR_RSB was already sent by
#    the driver when starting the MCU (slatersb_slateup_work in the
#    AFTER_POWERUP); here, with is_cnfgrd, store_enable queues
#    slatersb_enable_rsb -> SLATERSB_ENABLE to the MCU. It is done after BT so
#    as not to put it at risk: if this resets, the persistent log
#    (/var/log/dace-slate-mcu.log) will say 'hci0 READY' before, i.e. the culprit
#    is the ENABLE (not the CONFIGR or the MCU start).
n=0
while [ -e "$RSB" ] && [ "$n" -lt 5 ]; do
    if echo 1 > "$RSB" 2>/dev/null; then
        log "crown: RSB enabled (enable=1 ok, attempt $n)"
        break
    fi
    log "crown: enable=1 -> ENOMEDIUM (attempt $n; no CONFIGR_RSB yet)"
    n=$((n + 1))
    sleep 2
done
if [ -e "$RSB" ] && [ "$n" -ge 5 ]; then
    log "WARNING: crown: enable not accepted x5 (look for 'slatersb' in dmesg)"
fi

exit 0
