# Aurora vibrator path: ffmemless via cs40l26's FF_PERIODIC sine. We
# build on top of meta-asteroid's:
#   - 0003-ffmemless-accept-any-FF-capability    (cs40l26 selection)
#   - 0004-ffmemless-add-FF_PERIODIC-fallback... (default fall-back
#     effect uploads on devices without FF_CONSTANT/FF_RUMBLE)
#   - 0005-ffmemless-fire-single-shot-and-honor-haptic.duration
#     (single-shot events fire, and haptic.duration in events.d
#     drives the kernel-side effect length)
#
# With 0005 every event hits ffmemless's start path and plays the
# default fall-back sine, with a duration set by haptic.duration in
# events.d. Different events deserve distinct durations, so override
# them here. The defaults below are tuned for cs40l26's BUZZGEN whose
# single-shot ceiling is ~1020ms (255 * 4ms).

FILESEXTRAPATHS:prepend:aurora := "${THISDIR}/ngfd:"

do_install:append:aurora() {
    # Aurora vibrates through ffmemless only; the droid-vibrator plugin
    # isn't installed (Android-13's vibrator HAL is a dead stub here), so
    # drop it from plugins-optional to avoid a spurious
    # "unable to open plugin 'droid-vibrator'" ERROR at every ngfd start.
    sed -i 's/droid-vibrator;//' ${D}/usr/share/ngfd/ngfd.ini

    # Press is a button-tap acknowledgement -- short and crisp.
    f=${D}/usr/share/ngfd/events.d/press.ini
    if [ -f "$f" ]; then
        sed -i '/^haptic\.duration/d; /^sound\.repeat/d' "$f"
        echo 'haptic.duration = 80' >> "$f"
    fi

    # Normal notification -- noticeable but unobtrusive.
    f=${D}/usr/share/ngfd/events.d/notif_normal.ini
    if [ -f "$f" ]; then
        sed -i '/^haptic\.duration/d; /^sound\.repeat/d' "$f"
        echo 'haptic.duration = 400' >> "$f"
    fi

    # Strong notification -- distinctly longer.
    f=${D}/usr/share/ngfd/events.d/notif_strong.ini
    if [ -f "$f" ]; then
        sed -i '/^haptic\.duration/d; /^sound\.repeat/d' "$f"
        echo 'haptic.duration = 800' >> "$f"
    fi

    # Alarm and ringtone are already set up with sound.repeat = true
    # upstream, so they'll loop ffmemless. Pin the kernel-side per-shot
    # duration to the BUZZGEN ceiling so each pulse in the loop is as
    # long as the chip can manage.
    for e in alarm ringtone; do
        f=${D}/usr/share/ngfd/events.d/${e}.ini
        if [ -f "$f" ]; then
            sed -i '/^haptic\.duration/d' "$f"
            echo 'haptic.duration = 1000' >> "$f"
        fi
    done
}
