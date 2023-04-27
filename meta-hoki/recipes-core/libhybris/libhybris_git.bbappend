FILESEXTRAPATHS:prepend:hoki := "${THISDIR}/libhybris:"

SRCREV:hoki = "7c06a705b7c3ee5d8b663aa8689d18178884d7fb"

SRC_URI:remove:hoki = "file://0001-Add-EGL_OPENGL_ES3_BIT_KHR-define.patch;patchdir=.."
SRC_URI:remove:hoki = "file://0001-wayland-egl.pc.in-bump-Version-from-libhybris-s-0.1..patch;patchdir=.."
LIC_FILES_CHKSUM:hoki = "file://../LICENSE.Apache2;md5=3b83ef96387f14655fc854ddc3c6bd57"
