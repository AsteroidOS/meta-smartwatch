FILESEXTRAPATHS:prepend:ray := "${THISDIR}/libhybris:"

SRCREV:ray = "9cadeefe224aa2387cc88c9d17a374df2f265ba8"

SRC_URI:remove:ray = "file://0001-Add-EGL_OPENGL_ES3_BIT_KHR-define.patch;patchdir=.."
SRC_URI:append:ray = " file://0001-hybris-egl-Provide-eglCreatePlatformWindowSurface.patch;patchdir=.."

FILESEXTRAPATHS:prepend:firefish := "${THISDIR}/libhybris:"

SRCREV:firefish = "9cadeefe224aa2387cc88c9d17a374df2f265ba8"

SRC_URI:remove:firefish = "file://0001-Add-EGL_OPENGL_ES3_BIT_KHR-define.patch;patchdir=.."
SRC_URI:append:firefish = " file://0001-hybris-egl-Provide-eglCreatePlatformWindowSurface.patch;patchdir=.."
