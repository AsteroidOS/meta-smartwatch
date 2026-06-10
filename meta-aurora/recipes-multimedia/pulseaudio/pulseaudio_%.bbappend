# Aurora runs on a Halium-13 Android base. meta-asteroid's stock bbappend adds
# pulseaudio-modules-droid-jb2q to pulseaudio-server's RDEPENDS for every
# hybris-machine, but jb2q's audio_policy_conf parser only handles Android <=
# 10 schemas. Swap jb2q for pulseaudio-modules-droid (RPROVIDED by the
# merhybris recipe, the only provider in the aurora layer set) which parses
# A11+ schemas, plus the HIDL bridge and the audiosystem-passthrough helper
# that the A12+ binderized audio HAL needs.
RDEPENDS:pulseaudio-server:remove:aurora = "pulseaudio-modules-droid-jb2q"
RDEPENDS:pulseaudio-server:append:aurora = " pulseaudio-modules-droid pulseaudio-modules-droid-hidl audiosystem-passthrough"
