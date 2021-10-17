FILESEXTRAPATHS:prepend:harmony := "${THISDIR}/asteroid-launcher:"
SRC_URI:append:harmony = " file://default.conf \
                           file://0001-compositor-Fix-new-window-animation-on-screen-rotate.patch \
                           file://0002-GestureFilterArea-Fix-swipe-interaction-on-rotated-s.patch"
FILESEXTRAPATHS:prepend:inharmony := "${THISDIR}/asteroid-launcher:"
SRC_URI:append:inharmony = " file://default.conf"
