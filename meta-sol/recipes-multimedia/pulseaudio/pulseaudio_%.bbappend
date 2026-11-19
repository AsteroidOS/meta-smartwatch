# meta-asteroid adds pulseaudio-modules-droid-jb2q for every hybris machine,
# but jb2q's audio_policy_configuration parser only handles Android <= 10
# schemas. On sol's A14 base it aborts on the primary module's first mixPort:
#
#   conversion.c: Unknown flags entries: AUDIO_OUTPUT_FLAG_PRIMARY
#                 AUDIO_OUTPUT_FLAG_FAST
#   config-parser-xml.c: parsing aborted at line 34
#
# PulseAudio then has no card, falls back to module-always-sink, and every
# stream goes to a null sink: the UI works and nothing is audible.
#
# pulseaudio-modules-droid parses A11+ schemas. The HIDL bridge and
# audiosystem-passthrough are what the A12+ binderized audio HAL needs.
RDEPENDS:pulseaudio-server:remove:sol = "pulseaudio-modules-droid-jb2q"
RDEPENDS:pulseaudio-server:append:sol = " pulseaudio-modules-droid pulseaudio-modules-droid-hidl audiosystem-passthrough"

# default.pa is stock except for an explicit config= on module-droid-card.
# libdroid-util searches four fixed paths and sol keeps its policy one level
# below them, in /vendor/etc/audio/sku_monaco/. sol-audio-policy writes the
# file named here before the user session starts.
FILESEXTRAPATHS:prepend:sol := "${THISDIR}/pulseaudio:"
