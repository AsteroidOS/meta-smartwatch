# Aurora vibrates through cs40l26's FF_PERIODIC sine via ffmemless, with
# meta-asteroid's 0003-ffmemless-support-FF_PERIODIC-only-haptic-drivers
# patch. Single-shot events play the default fall-back effect for
# haptic.duration ms, set per event below. Alarm and ringtone play the
# clock and ringtone effects from 50-ffmemless.ini, which repeat with a
# pause until the client stops them. cs40l26's BUZZGEN caps a single
# effect at ~1020ms (255 * 4ms).

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
}
