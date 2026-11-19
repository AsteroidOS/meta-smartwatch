#!/bin/sh
# /usr/libexec/sol-wait-composer.sh
#
# Block until the Android container's composer is running, so asteroid-launcher
# does not start without a hwcomposer. The composer lives inside LXC and has no
# unit; sol-lxc-android.service is Type=simple and active as soon as lxc-start
# runs, long before Android init reaches class_start hal. Without this wait the
# launcher fails with "failed to get hwcomposer service" and hits the start
# rate limit.
#
# Matching is on argv[0] from /proc/PID/cmdline:
#   - Anchored, not a substring, so a shell whose command line contains the
#     path (including this script) never matches.
#   - cmdline, not /proc/PID/exe: exe needs PTRACE_MODE_READ and returns EACCES
#     for root processes to this unprivileged service. cmdline is world-readable.
#
# cmdline has no trailing newline, so `read` returns non-zero at EOF even after
# filling the variable. `read ... || continue` would discard every match.
#
# Bounded wait: on timeout it exits 0 and lets the launcher try anyway. The
# launcher's restart policy (50-sol-wait-for-composer.conf) covers the gap
# until the AIDL service registers.
set -u

COMPOSER=/vendor/bin/hw/vendor.qti.hardware.display.composer-service
TIMEOUT=${SOL_COMPOSER_TIMEOUT:-120}

composer_running() {
    for f in /proc/[0-9]*/cmdline; do
        # No `|| continue`: see the note above about read and EOF.
        read -r argv < "$f" 2>/dev/null
        case "$argv" in
            "$COMPOSER"*) return 0 ;;
        esac
    done
    return 1
}

# A builtin read per entry, no fork: this walks ~300 processes a round.
i=0
while [ "$i" -lt "$TIMEOUT" ]; do
    if composer_running; then
        # The service registers on vndbinder shortly after exec; a failed
        # lookup costs a full restart cycle.
        sleep 2
        echo "sol-wait-composer: composer up after ${i}s"
        exit 0
    fi
    sleep 2
    i=$((i + 2))
done

echo "sol-wait-composer: no composer after ${TIMEOUT}s, starting anyway"
exit 0
