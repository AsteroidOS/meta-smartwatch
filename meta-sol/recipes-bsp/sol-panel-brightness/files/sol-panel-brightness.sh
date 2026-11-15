#!/bin/sh
# Tell userspace the real brightness range of this panel.
#
# sol's panel driver packs two values into one backlight level:
#
#     /* We put NBM brightness at lower 16 bits and AOD brightness at higher 16 bits */
#     bl_lvl      = brightness & 0xFFFF;
#     doze_bl_lvl = (brightness >> 16) & 0xFFFF;
#
# qcom,mdss-brightness-max-level = 0xFF01FF means normal up to 0x1FF and AOD up
# to 0xFF, but the driver advertises 16712191 as max_brightness. mce scales
# against that, so most of the value lands in the AOD half: "10%" wrote
# 1671219 -> nbm 32819, aod 25, and raising brightness past half dimmed it.
#
# Bind-mount the normal-brightness maximum over the attribute before anything
# reads it. mce then maps 10%..100% onto nbm 51..511 with aod 0.
#
# The driver cannot be fixed here: sol loads the stock msm_drm.ko from
# vendor_dlkm, not the one linux-sol-modules builds. mce reads max_brightness
# with no override, but mce is shared by every watch; this is one panel's quirk.
set -u

BL=/sys/class/backlight/panel0-backlight/max_brightness
OUT=/run/sol-panel-max-brightness

[ -e "$BL" ] || { echo "sol-panel-brightness: $BL absent, nothing to correct"; exit 0; }

raw=$(cat "$BL" 2>/dev/null)
case "$raw" in
    ''|*[!0-9]*) echo "sol-panel-brightness: '$raw' is not a number, leaving it alone"; exit 0 ;;
esac

# The normal-brightness maximum is the low 16 bits; anything above that is the
# AOD field. If the panel already advertises a plain value there is nothing to do.
nbm=$((raw & 0xFFFF))
if [ "$nbm" -eq "$raw" ]; then
    echo "sol-panel-brightness: max_brightness=$raw is already a plain value"
    exit 0
fi

echo "$nbm" > "$OUT"
chmod 0444 "$OUT"
if mount --bind "$OUT" "$BL" 2>/dev/null; then
    echo "sol-panel-brightness: max_brightness $raw -> $(cat "$BL") (normal-brightness range only)"
else
    echo "sol-panel-brightness: could not bind $OUT over $BL"
fi
