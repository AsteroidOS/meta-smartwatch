#!/bin/sh
# BRING-UP ONLY. Remove once usb-moded works on sol (see the recipe header).
#
# Does the one part of usb-moded's adb_mode we need, without the cable-state
# dependency that fails on sol ("usb_moded: udev io watch disabled", after
# which usb-moded never calls set_udc).
#
# adbd-prepare.service has mounted FunctionFS at /dev/usb-ffs/adb. ep1 appears
# once adbd has written its descriptors (FFS_ACTIVE); binding the UDC before
# that fails ffs_func_bind() with ENODEV.
G=/config/usb_gadget/g1
log() { echo "sol-bringup-adb: $1"; }

[ -d "$G" ] || { log "FATAL: $G missing (init_gfs did not run?)"; exit 1; }

i=0
while [ ! -e /dev/usb-ffs/adb/ep1 ]; do
    i=$((i+1))
    [ "$i" -gt 100 ] && { log "FATAL: timed out waiting for /dev/usb-ffs/adb/ep1"; exit 1; }
    sleep 0.1
done
log "FunctionFS ready (ep1 present after $((i))00ms)"

if [ ! -e "$G/configs/b.1/ffs.adb" ]; then
    ln -s "$G/functions/ffs.adb" "$G/configs/b.1/ffs.adb" || { log "FATAL: symlink failed"; exit 1; }
    log "linked ffs.adb into configs/b.1"
fi

udc=$(ls /sys/class/udc 2>/dev/null | head -n 1)
[ -n "$udc" ] || { log "FATAL: no UDC in /sys/class/udc"; exit 1; }

cur=$(cat "$G/UDC" 2>/dev/null)
if [ -n "$cur" ]; then
    log "UDC already bound to '$cur', leaving alone"
    exit 0
fi

if echo "$udc" > "$G/UDC" 2>/dev/null; then
    log "bound UDC=$udc -- adb gadget should be live"
else
    log "FATAL: writing UDC=$udc failed"
    exit 1
fi
