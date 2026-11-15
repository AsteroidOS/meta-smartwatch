#!/bin/sh
# /usr/bin/sol-usb-network.sh
#
# USB networking (and ssh over USB) through the NCM gadget function.
#
# usb-moded cannot do it on this device family: its developer_mode wants RNDIS,
# which this kernel lacks
#     usb_moded: /config/usb_gadget/g1/functions/rndis.rndis: mkdir failed
# and its udev monitor dies at startup ("udev io watch disabled"), so it never
# selects a mode. That is a shared usb-moded bug. usb-moded is masked on sol, so
# this script is the only configfs writer and can configure the gadget directly.
#
# 192.168.43.2, not aurora's 192.168.42.2, so both watches can share a host.
# sshd.socket already listens on [::]:22; it only needed an interface.
set -u

G=/config/usb_gadget/g1
ADDR="${SOL_USB_ADDR:-192.168.43.2/24}"
LOGF=/var/log/sol-usb-network.log

log() {
    echo "sol-usb-network: $1"
    echo "$(cut -d. -f1 /proc/uptime)s $1" >> "$LOGF" 2>/dev/null || true
}

# --- stable, per-device MAC so the IPv6 link-local never changes ------------
#
# Stable per-device MAC, so the IPv6 link-local (EUI-64 from usb0's MAC) is
# the same every boot and can be cached by the host. The kernel otherwise picks
# a random MAC each time ncm is created. Derived from the DSN, falling back to
# the hostname. First octet 0x02: locally administered, unicast.
stable_mac() {
    dsn=""
    [ -r /sys/firmware/devicetree/base/chosen/config/dsn ] && \
        dsn="$(tr -d '\0' < /sys/firmware/devicetree/base/chosen/config/dsn)"
    [ -n "$dsn" ] || dsn="$(cat /etc/hostname 2>/dev/null)"
    [ -n "$dsn" ] || dsn="asteroid"
    h=$(printf '%s' "$dsn" | md5sum | cut -c1-10)
    printf '02:%s:%s:%s:%s:%s\n' \
        "$(printf '%s' "$h" | cut -c1-2)" "$(printf '%s' "$h" | cut -c3-4)" \
        "$(printf '%s' "$h" | cut -c5-6)" "$(printf '%s' "$h" | cut -c7-8)" \
        "$(printf '%s' "$h" | cut -c9-10)"
}

pin_mac() {
    want=$(stable_mac)
    cur=$(tr -d '\0' < "$G/functions/ncm.0/dev_addr" 2>/dev/null)
    if [ "$cur" = "$want" ]; then
        log "dev_addr already pinned to $want"
        return 0
    fi
    if echo "$want" > "$G/functions/ncm.0/dev_addr" 2>/dev/null; then
        log "pinned dev_addr=$want (was ${cur:-unset}) -> stable link-local"
    else
        log "WARNING: could not pin dev_addr (function bound?); link-local will vary per boot"
    fi
}

[ -d "$G" ] || { log "FATAL: $G missing (init_gfs did not run?)"; exit 1; }

# sol's gadget carries only ffs.adb, so ncm.0 must be created. A failing mkdir
# means the kernel has no NCM (CONFIG_USB_CONFIGFS_NCM).
if [ ! -d "$G/functions/ncm.0" ]; then
    if ! mkdir "$G/functions/ncm.0" 2>/dev/null; then
        log "kernel has no NCM configfs function -- cannot provide USB networking"
        exit 0
    fi
    log "created ncm.0 function"
    # dev_addr only takes effect while the function is neither bound nor up:
    # writing it while bound errors out, and writing it while the netdev is up
    # stores the attribute but leaves usb0's MAC unchanged.
    pin_mac
fi

# Pin before linking, with usb0 down (see above).
if [ -e /sys/class/net/usb0 ]; then ip link set usb0 down 2>/dev/null; fi
pin_mac

if [ -e "$G/configs/b.1/ncm.0" ]; then
    log "ncm.0 already linked; leaving the gadget alone"
else
    U=$(ls /sys/class/udc 2>/dev/null | head -1)
    [ -n "$U" ] || { log "FATAL: no UDC"; exit 1; }

    # Adding a function needs the gadget unbound, which briefly drops adb.
    # ffs.adb stays linked, so the result is an adb + network composite.
    log "unbinding UDC=$U to add ncm.0"
    echo "" > "$G/UDC" 2>/dev/null
    sleep 1
    ln -sf "$G/functions/ncm.0" "$G/configs/b.1/ncm.0" 2>/dev/null \
        && log "linked ncm.0" || { log "FATAL: could not link ncm.0"; echo "$U" > "$G/UDC" 2>/dev/null; exit 1; }
    sleep 1
    echo "$U" > "$G/UDC" 2>/dev/null && log "rebound UDC" || log "FATAL: rebind failed"
    sleep 3
fi

# usb0 appears only once the function is bound.
i=0
while [ ! -e /sys/class/net/usb0 ] && [ "$i" -lt 15 ]; do i=$((i + 1)); sleep 1; done
if [ ! -e /sys/class/net/usb0 ]; then
    log "usb0 never appeared after linking ncm.0"
    exit 1
fi

ip link set usb0 up 2>/dev/null
# The address may already be there; that is not a failure.
ip addr add "$ADDR" dev usb0 2>/dev/null || true

log "usb0 up: $(ip -4 addr show usb0 2>/dev/null | grep -o 'inet [0-9./]*' | head -1)"
log "sshd.socket=$(systemctl is-active sshd.socket 2>/dev/null) -- host side needs: ip addr add 192.168.43.1/24 dev <usb-iface>; ip link set <usb-iface> up"
