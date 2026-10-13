#!/bin/sh
# dace: EXPLICIT load of the vendor chain from /etc/modules-load.d/dace-post-rootfs.conf
#
# Why is systemd-modules-load not enough? Because kmod applies the blacklist in
# /etc/modprobe.d/00-dace-vendor-blacklist.conf as a *deny-list* also to the
# `modprobe` that systemd-modules-load runs: the journal shows
#     Module 'wlan' is deny-listed (by kmod)
# and it does NOT load it. In contrast, an explicit `modprobe` from a shell/script
# DOES ignore the deny-list (the blacklist only affects autoload by alias).
# That is why this service runs modprobe by hand, in order, tolerating failures.
#
# The blacklist is STILL necessary: it is what stops udev from loading the 76
# vendor .ko's by modalias during coldplug (that took the SoC down to EDL).

CONF=/etc/modules-load.d/dace-post-rootfs.conf
[ -r "$CONF" ] || exit 0

# Dependencies that live in the initramfs but by ordering may not be there
# (cfg80211 is pulled in by wlan as a dep, no need to force it).
# NOTE: some modprobe BLOCKS (seen with pmw5100-spmi_dlkm, which has no device
# in this DT): without a timeout the service stays in 'activating' forever.
# timeout avoids that hang (TMOUT does not apply to modprobe).
rc=0
while IFS= read -r line; do
    case "$line" in
        ''|\#*) continue ;;
    esac
    # in case the line carries a trailing comment
    mod=${line%%#*}
    mod=$(echo "$mod" | tr -d ' \t')
    [ -n "$mod" ] || continue
    if timeout 8 modprobe "$mod" 2>/dev/null; then
        echo "dace-modules-load: $mod OK"
    else
        echo "dace-modules-load: $mod FAIL (o timeout)"
        rc=1
    fi
done < "$CONF"

# Best-effort: a module that fails (e.g. google_wlan_mac, which needs the
# /chosen/config node from Google's bootloader) must NOT mark the service as
# 'failed'. The others have already been loaded.
[ "$rc" = 1 ] && echo "dace-modules-load: some module failed (see above); continuing"
exit 0
