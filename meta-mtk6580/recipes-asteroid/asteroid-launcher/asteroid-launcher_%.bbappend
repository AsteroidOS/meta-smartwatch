FILESEXTRAPATHS_prepend_harmony := "${THISDIR}/asteroid-launcher:"
SRC_URI_append_harmony = " file://default.conf \
                           file://0001-compositor-Fix-new-window-animation-on-screen-rotate.patch \
                           file://0002-GestureFilterArea-Fix-swipe-interaction-on-rotated-s.patch"
