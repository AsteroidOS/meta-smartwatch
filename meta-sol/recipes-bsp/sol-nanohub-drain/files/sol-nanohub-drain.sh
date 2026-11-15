#!/bin/sh
# /usr/bin/sol-nanohub-drain.sh
#
# Keeps the nanohub buffer pool from being exhausted by channels nothing reads.
#
# google-modules/nanohub gives all 30 channels one shared pool of 20 buffers
# (READ_QUEUE_DEPTH), and a buffer returns to it only when userspace read()s it.
# nanohub_process_buffer() drops packets for unopened channels only when
# id == ID_NANOHUB_CLIENT:
#
#     if (id == ID_NANOHUB_CLIENT && atomic_read(&io->busy) != 1) {
#             pr_info("nanohub: dropping packet for /dev/nanohub/%d\n", id);
#     } else if (!io->is_kernel) {
#             nanohub_io_put_buf(io, *buf);      /* queued unconditionally */
#
# and nanohub_release()'s discard sweep is CLIENT-only too. So any other
# channel nobody opens accumulates buffers. Once all 20 are stranded (on aurora,
# all on /dev/nanohub_metrics), the reader kthread's
# nanohub_io_get_buf(&data->free_pool, false) returns NULL and the driver stops
# reading the MCU: every sensor dies, sensorfwd stops answering D-Bus, mce
# blocks holding a wakelock. All systemd units still report healthy.
#
# The proper fix is in the driver (extend the busy guard and release sweep to
# every non-kernel channel), but nanohub is shared with aurora and other
# Google watches, so this is a device-local workaround. The logged byte
# counters show which channels really carry traffic.
set -u

INTERVAL=60          # how often to report counters
GRACE=90             # see below -- do NOT remove
STATE=/run/sol-nanohub-drain

# The container HALs (displayoffload, mcu_mgmtd, chre) open their nodes some
# time after sol-lxc-android.service starts. Scanning holders at once would
# drain nodes a HAL is about to claim and steal its packets; GRACE prevents that.

# Candidate channels; the holder check skips any a process already reads
# (mcu_mgmtd holds debug_log). _brightness, _display and _render are left out
# because displayoffload reads them; if it is ever stubbed, add them back or the
# pool leaks.
NODES="/dev/nanohub_metrics
/dev/nanohub_console
/dev/nanohub_rpc0
/dev/nanohub_rpc1
/dev/nanohub_debug_log
/dev/nanohub_pele
/dev/nanohub_bt"

log() { echo "sol-nanohub-drain: $1"; }

# Refuse to touch a node another process already has open. Scans /proc/*/fd
# (no fuser on this image); validated once at startup against nodes the
# multihal holds, so a broken scan cannot pass for "nothing is listening".
holder_of() {
    want_dev=$(stat -c '%t:%T' "$1" 2>/dev/null)
    want_base=$(basename "$1")
    for l in /proc/[0-9]*/fd/*; do
        t=$(readlink "$l" 2>/dev/null) || continue
        # Cheap filter first; only the handful of nanohub fds get stat'd.
        case "$t" in *nanohub*) ;; *) continue ;; esac
        # Match on the device number too: a holder inside the container can
        # show a different path for the same node.
        got_dev=$(stat -Lc '%t:%T' "$l" 2>/dev/null)
        if [ "$got_dev" = "$want_dev" ] || [ "$(basename "$t")" = "$want_base" ]; then
            p=${l#/proc/}; p=${p%%/*}
            echo "$p"
            return 0
        fi
    done
    return 1
}

mkdir -p "$STATE"

log "waiting ${GRACE}s for container HALs to claim their nodes before deciding"
sleep "$GRACE"

# --- startup sanity: can this scan see ANY nanohub opener at all? -------------
seen=""
for n in /dev/nanohub /dev/nanohub_comms; do
    [ -e "$n" ] || continue
    h=$(holder_of "$n") && seen="$seen $n(pid $h)"
done
if [ -n "$seen" ]; then
    log "fd scan validated, sees existing openers:$seen"
else
    log "WARNING: fd scan found no opener on any nanohub node. Either nothing has one (possible) or the scan cannot see them (method broken). Treat the skip decisions below with suspicion."
fi

started=""
for n in $NODES; do
    if [ ! -e "$n" ]; then
        log "skip $n (absent)"
        continue
    fi
    if h=$(holder_of "$n"); then
        log "SKIP $n -- already held by pid $h; draining it would steal that reader's packets"
        continue
    fi

    # One drainer per node. Each read is a fresh open/read/close that blocks in
    # nanohub_io_get_buf(io, true); the close is harmless (the release sweep is
    # CLIENT-only).
    (
        name=$(basename "$n")
        total=0; pkts=0; iters=0
        # holder_of() forks per fd, so it stays out of the hot path: dd blocks
        # on traffic, EOF backs off 60 s, and the holder is re-checked every 20
        # iterations. A per-read check cost ~4.5 mA.
        while :; do
            got=$(dd if="$n" bs=255 count=1 2>/dev/null | wc -c)
            if [ "${got:-0}" -gt 0 ]; then
                total=$((total + got))
                pkts=$((pkts + 1))
                echo "$pkts $total" > "$STATE/$name"
            else
                sleep 60
            fi
            iters=$((iters + 1))
            if [ $((iters % 20)) -eq 0 ] && hn=$(holder_of "$n"); then
                log "STOPPING drain of $n -- pid $hn has claimed it (drained ${pkts}pkt/${total}B)"
                exit 0
            fi
        done
    ) &
    started="$started $(basename "$n")"
    log "draining $n"
done

[ -n "$started" ] || { log "nothing to drain; exiting"; exit 0; }
log "drainers running for:$started"

# Periodic report: a climbing counter is a channel that would have leaked
# buffers from the shared pool.
while :; do
    sleep "$INTERVAL"
    line=""
    for f in "$STATE"/*; do
        [ -e "$f" ] || continue
        read -r p t < "$f" 2>/dev/null || continue
        line="$line $(basename "$f")=${p}pkt/${t}B"
    done
    [ -n "$line" ] && log "drained:$line"
done
