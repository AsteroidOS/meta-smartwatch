# sol runs Android 14 system libraries under libhybris. Everything here is
# :sol-scoped, following meta-aurora's libhybris bbappend.
#
# libhybris-a13-hooks.patch is Florent's, copied unchanged from meta-aurora with
# his Signed-off-by. It bridges libc/runtime symbols the Android system libs
# import and upstream libhybris lacks (ARM EHABI unwinder, memfd_create,
# android_get_device_api_level, __system_properties_init, fdsan members). All
# exist unchanged in A14, and both layers pin the same SRCREV. Since it is not
# device-specific, the tidy end state is one copy in meta-asteroid; that is a
# fleet-wide change for a separate PR.
FILESEXTRAPATHS:prepend:sol := "${THISDIR}/libhybris:"
SRC_URI:append:sol = " file://libhybris-a13-hooks.patch \
                       file://libhybris-skip-bionic-property-init-without-area.patch \
                       file://libhybris-sol-disable-ubwc-usage.patch"

# No SRCREV bump for the AIDL composer. sol's vendor declares only
# android.hardware.graphics.composer3 (AIDL), and upstream HEAD has
# AidlComposerHal.cpp, but this recipe sets S = "${WORKDIR}/git/hybris", so
# compat/hwc2/ is never built. The composer code that runs is the prebuilt
# /android/hybris/system/lib/libhwc2_compat_layer.so from the Halium image;
# changing it means rebuilding that image, not bumping this SRCREV.

# Library search path for the hybris linker.
#
# meta-asteroid's default puts /vendor/lib before /system/lib, so for a library
# shipped in both, vendor's older VNDK copy wins. On sol that broke the launcher:
#     cannot locate symbol "_ZN7android26openDeclaredPassthroughHalERKNS_8String16ES2_i"
#     referenced by "libbinder_ndk.so"
# (libbinder.so from /vendor/lib, libbinder_ndk.so from /system/lib). /system/lib
# first fixes the whole class; vendor still wins for what system lacks.
#
# /apex/com.android.runtime/lib/bionic: sol's bionic lives in an APEX, loop-mounted
#   by sol-vendor-mount; /system/lib/libc.so is only a symlink into it.
# /vendor/lib/egl: the Adreno EGL driver dlopens eglSubDriverAndroid.so from here.
# /vendor/lib/hw: passthrough HALs, including the gralloc4 mapper.
# /usr/libexec/hal-droid/system/lib: empty on sol (no android-system), kept last.
#
# asteroid-launcher sets its own HYBRIS_LD_LIBRARY_PATH, as on aurora. This
# default covers sensorfwd, ngfd and bluebinder.
EXTRA_OECONF:append:sol = " --with-default-hybris-ld-library-path=/android/hybris/system/lib:/system/lib:/vendor/lib:/vendor/lib/egl:/vendor/lib/hw:/apex/com.android.runtime/lib/bionic:/usr/libexec/hal-droid/system/lib"

# libhybris-sol-disable-ubwc-usage.patch, aurora's patch by Florent with his
# Signed-off-by; only a comment line naming the device differs.
# HWComposerNativeWindow ORs GRALLOC_USAGE_HW_COMPOSER into the buffer usage,
# which on Monaco's gralloc4 selects UBWC. The only SDE plane SDM has for the
# launcher's client-composition layer is plane 70, a DMA pipe without UBWC, so
# the kernel rejects the commit and the display stays off:
#     E SDM: DRMAtomicReq::Commit: drmModeAtomicCommit failed with error 22
#     E SDM: HWDeviceDRM::AtomicCommit: AtomicCommit failed with error -22 crtc 80
# The patch is not gated on HYBRIS_HWC_DISABLE_COMPOSER_USAGE (neither copy reads
# it, despite meta-aurora's comment); the gate is the per-machine SRC_URI append.
