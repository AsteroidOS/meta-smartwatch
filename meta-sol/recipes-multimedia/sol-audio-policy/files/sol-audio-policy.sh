#!/bin/sh
# Derive PulseAudio's droid audio policy configuration for sol from the vendor
# image's own, which module-droid-card cannot use as shipped:
#
#   * libdroid-util searches /odm/etc, /system/etc, /vendor/etc/audio and
#     /vendor/etc for audio_policy_configuration.xml. sol keeps it in
#     /vendor/etc/audio/sku_monaco/, so PulseAudio fell back to a null sink.
#
#   * sol declares LE Audio ports (AUDIO_DEVICE_{OUT,IN}_BLE_HEADSET), which are
#     newer than the module; pa_droid_profile_set_default_new() segfaults on
#     them. aurora declares none.
#
# The copy drops the BLE device ports, routes into them and BLE entries in
# other routes' sources; the rest passes through unchanged, so vendor updates
# carry over. Nothing is lost: BlueZ, not the Android policy, owns BT audio.
set -eu

DST=${1:-/etc/pulse/sol-audio-policy.xml}
SRC=

# Per-SKU directory first, then the conventional locations.
for candidate in /vendor/etc/audio/sku_*/audio_policy_configuration.xml \
                 /vendor/etc/audio/audio_policy_configuration.xml \
                 /vendor/etc/audio_policy_configuration.xml; do
    if [ -r "$candidate" ]; then
        SRC=$candidate
        break
    fi
done

if [ -z "$SRC" ]; then
    echo "sol-audio-policy: no vendor audio policy configuration found" >&2
    exit 1
fi

awk '
    # LE Audio device ports, children and all.
    /<devicePort[^>]*BLE/       { if ($0 ~ /\/>[[:space:]]*$/) next; drop_port = 1; next }
    drop_port                   { if (/<\/devicePort>/) drop_port = 0; next }

    # Routes into an LE Audio sink. These span more than one line and end on />.
    /<route[^>]*sink="[^"]*BLE/ { if ($0 ~ /\/>/) next; drop_route = 1; next }
    drop_route                  { if (/\/>/) drop_route = 0; next }

    # LE Audio sources listed in the routes that are kept.
    {
        gsub(/,[[:space:]]*BLE In/, "")
        gsub(/BLE In[[:space:]]*,/, "")
        print
    }
' "$SRC" > "$DST.new"

# Keep the previous file rather than install one without the primary output
# mixPort or with BLE ports left: PulseAudio reads it on every start.
if ! grep -q 'mixPort name="primary output"' "$DST.new"; then
    echo "sol-audio-policy: no primary output in result derived from $SRC" >&2
    rm -f "$DST.new"
    exit 1
fi
if grep -q BLE "$DST.new"; then
    echo "sol-audio-policy: BLE device ports survived filtering of $SRC" >&2
    rm -f "$DST.new"
    exit 1
fi

mv "$DST.new" "$DST"
echo "sol-audio-policy: derived $DST from $SRC"

# --- speaker protection ---------------------------------------------------
#
# PAL's speaker protection opens a second graph for the amplifier's VI feedback
# with every playback stream. Resuming a suspended sink over that path
# deadlocks PulseAudio: the process stays alive but every client times out at
# 30s. With module-suspend-on-idle fired first: enabled 5/5 wedged, disabled
# 5/5 clean. sol's vendor enables it in the resourcemanager variant this device
# selects; aurora's has 0 and the rest of the audio config matches.
#
# The CS40L26 amplifier still does excursion and thermal limiting in its own
# DSP; only PAL's host-side loop is switched off. /vendor is read-only, so the
# edited variants are bind-mounted over the originals.
RUNDIR=/run/sol-audio-policy
mkdir -p "$RUNDIR"

for rm_xml in /vendor/etc/audio/sku_*/resourcemanager_*.xml; do
    [ -r "$rm_xml" ] || continue
    grep -q '<speaker_protection_enabled>1</speaker_protection_enabled>' "$rm_xml" || continue

    copy="$RUNDIR/$(basename "$rm_xml")"
    sed 's|<speaker_protection_enabled>1</speaker_protection_enabled>|<speaker_protection_enabled>0</speaker_protection_enabled>|' \
        "$rm_xml" > "$copy"

    # Replace a bind from an earlier start rather than stack. umount exits 32
    # when nothing is mounted, the normal case, so ignore it.
    umount "$rm_xml" 2>/dev/null || :
    if mount --bind "$copy" "$rm_xml"; then
        echo "sol-audio-policy: speaker protection off in $(basename "$rm_xml")"
    else
        echo "sol-audio-policy: failed to bind $copy over $rm_xml" >&2
    fi
done
