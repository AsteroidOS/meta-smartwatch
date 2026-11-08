# dace: the crown (RSB) writes REL_WHEEL and its pushbutton emits KEY_MENU.
# The launcher did not consume them: without WheelHandler the app list does not
# move (the default style, 000-default-horizontal, does not have it) and without
# Keys.onPressed the button does nothing.
#
# IMPORTANT: the patch is against c8c4d4b. The base recipe uses SRCREV=AUTOREV,
# so we pin it here: otherwise a build re-fetching master can break `git apply`
# (changed context).
FILESEXTRAPATHS:prepend:dace := "${THISDIR}/asteroid-launcher:"
SRC_URI:append:dace = " file://0001-dace-crown.patch"
SRCREV:dace = "c8c4d4bd469c8ae5223f4bb7a58e3c96982eb1d9"