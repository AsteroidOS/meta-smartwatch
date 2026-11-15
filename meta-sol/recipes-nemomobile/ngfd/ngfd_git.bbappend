# sol vibrates through ffmemless on the CS40L26A's FF_PERIODIC sine, as aurora
# does (same chip, same meta-asteroid patches). See ngfd/50-ffmemless.ini for
# why the stock effect list cannot be used. BUZZGEN's single-shot ceiling is
# about 1020 ms (255 * 4 ms), so 1000 is the practical maximum per pulse.

FILESEXTRAPATHS:prepend:sol := "${THISDIR}/ngfd:"

do_install:append:sol() {
    # droid-vibrator is not installed; drop it from plugins-optional to avoid
    # an "unable to open plugin" error at every ngfd start.
    sed -i 's/droid-vibrator;//' ${D}/usr/share/ngfd/ngfd.ini

    # Button-tap acknowledgement: short and crisp.
    f=${D}/usr/share/ngfd/events.d/press.ini
    if [ -f "$f" ]; then
        sed -i '/^haptic\.duration/d; /^sound\.repeat/d' "$f"
        echo 'haptic.duration = 80' >> "$f"
    fi

    # Normal notification: noticeable but unobtrusive.
    f=${D}/usr/share/ngfd/events.d/notif_normal.ini
    if [ -f "$f" ]; then
        sed -i '/^haptic\.duration/d; /^sound\.repeat/d' "$f"
        echo 'haptic.duration = 400' >> "$f"
    fi

    # Strong notification: distinctly longer.
    f=${D}/usr/share/ngfd/events.d/notif_strong.ini
    if [ -f "$f" ]; then
        sed -i '/^haptic\.duration/d; /^sound\.repeat/d' "$f"
        echo 'haptic.duration = 800' >> "$f"
    fi

    # Alarm and ringtone already carry sound.repeat = true upstream, so they
    # loop ffmemless. Pin each pulse to the BUZZGEN ceiling.
    for e in alarm ringtone; do
        f=${D}/usr/share/ngfd/events.d/${e}.ini
        if [ -f "$f" ]; then
            sed -i '/^haptic\.duration/d' "$f"
            echo 'haptic.duration = 1000' >> "$f"
        fi
    done
}
