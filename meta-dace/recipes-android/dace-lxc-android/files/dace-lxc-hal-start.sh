#!/bin/sh
# Start the graphics HAL services of the Android container.
#
# WHY IT IS NEEDED: the T5 vendor .rc files declare the services
# (vendor.qti.hardware.display.composer / .allocator) but WITHOUT the
# 'interface' line, so the 'ctl.interface_start' that hwservicemanager emits for
# every getService() is not mapped to any service and init does NOT start them.
# Without them, libhybris/lipstick cannot create the composer client or allocate
# buffers -> the compositor blocks and the screen stays black (even if the CRTC
# is enabled and wayland-0 exists).
#
# The porting documentation calls this "starting the right android boot
# services". They are launched with the Android init's own mechanism (ctl.start),
# which respects SELinux, via lxc-attach (ABSOLUTE path: the container has no
# /bin/sh).
set -u
for i in $(seq 1 60); do
    # wait for the container to have hwservicemanager
    if lxc-attach -n android -- /system/bin/getprop 2>/dev/null | grep -q .; then
        break
    fi
    sleep 2
done

for svc in vendor.qti.hardware.display.allocator vendor.qti.hardware.display.composer; do
    lxc-attach -n android -- /system/bin/setprop ctl.start "$svc" 2>/dev/null
    echo "dace-lxc-hal-start: ctl.start $svc"
    sleep 2
done

# ─────────── BLUETOOTH (aurora path: vendor HAL + bluebinder) ───────────
# The Qualcomm BT HAL (/vendor/bin/hw/android.hardware.bluetooth@1.0-service-qti)
# is the one that does the whole real WCN3988 bring-up: chip power-up via
# /dev/btpower, wakeup + baud change over the UART (/dev/ttyHS0 with
# /sys/class/tty/ttyHS0/device/hs_uart_operation) and firmware download
# apbtfw11.tlv/apnv11.bin. bluebinder (host) consumes it over binder/hci_vhci.
# Its .rc has NO 'interface' line -> like allocator/composer, it must be started
# with ctl.start (the hwservicemanager ctl.interface_start does not map it and
# bluebinder stays in 'Waiting for bluetooth service' forever).
#
# TWO permissions that must be fixed (measured live 2026-09-19):
#  - the CONTAINER's /dev/ttyHS0 is crw------- root root (the host ueventd does
#    not rule the container's /dev tmpfs; stock has no rule for ttyHS0). The HAL
#    runs as user 'bluetooth' -> EACCES opening the UART.
#  - /sys/class/tty/ttyHS0/device/hs_uart_operation is -rw-r--r-- root root (it
#    is created by msm_geni_serial) and the HAL needs to write it -> EACCES.
# Without these two the HAL dies with 'UART INIT failed'.
lxc-attach -n android -- /system/bin/toybox chmod 0666 /dev/ttyHS0 2>/dev/null \
    && echo "dace-lxc-hal-start: /dev/ttyHS0 -> 0666 (container)"
lxc-attach -n android -- /system/bin/toybox chmod 0666 /sys/class/tty/ttyHS0/device/hs_uart_operation 2>/dev/null \
    && echo "dace-lxc-hal-start: hs_uart_operation -> 0666 (container)"
lxc-attach -n android -- /system/bin/setprop ctl.start vendor.bluetooth-1-0-qti 2>/dev/null
sleep 3
lxc-attach -n android -- /system/bin/getprop init.svc.vendor.bluetooth-1-0-qti 2>/dev/null \
    | sed 's/^/dace-lxc-hal-start: BT HAL init.svc.vendor.bluetooth-1-0-qti=/'

# TOUCH: load the RAYDIUM driver NOW (not earlier). With a touch driver present
# from boot, the Qualcomm composer-service dies with SIGSEGV ~0.2 s after
# creating /dev/socket/pps and the UI is left without a composer (the display
# stack reacts to the panel/touch). It is left for the end.
#
# Zinitix touch: VENDOR CHAIN (stock .ko, see AGENTS.md §5). Requires the kernel
# with the cfi_check ABI slot + SCS (dace-module-cfi-abi-slot.patch,
# CONFIG_SHADOW_CALL_STACK). Our mainline zinitix no longer exists (=n in the
# fragment). Exact order measured live:
#   bridge rpmsg -> bridge -> mobvoi_rpmsg -> mobvoi -> zinitix-i2c
modprobe panel_event_notifier 2>/dev/null
VD=/usr/lib/dace-vendor-modules
for m in slate_events_bridge_rpmsg slate_events_bridge slate_mobvoi_rpc_rpmsg slate_mobvoi_rpc zinitix-i2c; do
    insmod "$VD/$m.ko" 2>/dev/null \
        && echo "dace-lxc-hal-start: vendor $m loaded" \
        || echo "dace-lxc-hal-start: warning, could not load $m"
done
sleep 3
grep -q "zinitix_ts" /proc/bus/input/devices 2>/dev/null && echo "dace-hal: touch PRESENT (zinitix_ts vendor)" \
    || echo "dace-hal: touch ABSENT"

# ─────────────────── BATTERY (E1): smblite EARLY ───────────────────
# In stock `qpnp-smblite-main` goes in the ramdisk (long before the MCU). The
# remote-FG registers for GMI_SLATE_EVENT_QBG and requests data; if the MCU only
# streams to clients registered before its boot, loading it late
# (dace-slate-mcu) would not be enough. Here the STOCK bridge is already loaded
# (above). `insmod -f`: our .ko (:tp, google-eos) carries the seb_* CRC of the
# google-eos bridge, but on the watch the STOCK bridge runs (zinitix needs it)
# -> only that check must be skipped (it is OUR module, without SCS: safe).
# Requires the DTB with `dpdm-supply` removed from the smblite node
# (dace-boot-images), otherwise the probe hangs in devm_regulator_get("dpdm").
modprobe gvotable 2>/dev/null
KREL=$(uname -r)
if ! grep -q '^qpnp_smblite_main ' /proc/modules; then
    if [ -e "/lib/modules/$KREL/vendor/qpnp-smblite-main.ko" ]; then
        insmod -f "/lib/modules/$KREL/vendor/qpnp-smblite-main.ko" 2>/dev/null \
            && echo "dace-lxc-hal-start: smblite loaded (battery psy early)" \
            || echo "dace-lxc-hal-start: warning, could not load smblite"
    else
        echo "dace-lxc-hal-start: warning, missing /lib/modules/$KREL/vendor/qpnp-smblite-main.ko"
    fi
fi

# Check: that the service responds (the client requests it over the bus)
sleep 5
lxc-attach -n android -- /system/bin/sh -c '
    for s in allocator composer; do
        if ps -A 2>/dev/null | grep -q "$s-service"; then echo "dace-hal: $s-service RUNNING"; else echo "dace-hal: $s-service MISSING"; fi
    done' 2>/dev/null
