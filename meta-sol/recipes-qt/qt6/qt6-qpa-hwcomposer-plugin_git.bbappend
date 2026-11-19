# Present the always-on face updates in DOZE, enabled by
# QPA_HWC2_FORCE_DOZE_UPDATE=doze in asteroid-launcher-configs' 20-device.conf.
FILESEXTRAPATHS:prepend:sol := "${THISDIR}/qt6-qpa-hwcomposer-plugin:"
SRC_URI:append:sol = " file://0001-hwc2-optionally-present-ambient-updates-in-DOZE.patch;striplevel=2"
