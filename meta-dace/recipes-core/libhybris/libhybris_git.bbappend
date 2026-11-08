# Dace is AsteroidOS's first Android-13 device. Two dace-only patches:
#
# libhybris-a13-hooks.patch -- a handful of A13 /system libs import symbols
# that upstream libhybris doesn't bridge, so consumers loaded by
# asteroid-launcher SIGSEGV. The patch maps unwinders and memfd_create to
# host glibc/libgcc, backs android_get_device_api_level with
# ro.build.version.sdk, no-ops __system_properties_init, and stubs the
# fdsan family (libbase unique_fd reaches the create_owner_tag GOT slot
# through gralloc4 dmabuf fds; with no stub, Qt's QSGRenderThread crashes
# PC=0).
#
# libhybris-dace-disable-ubwc-usage.patch -- HWComposerNativeWindow
# default-ORs GRALLOC_USAGE_HW_COMPOSER into the buffer usage, which on
# Monaco's QTI gralloc4 path triggers UBWC allocation. Dace's only
# launcher-eligible SDE plane (plane 70, DMA pipe) doesn't support UBWC,
# so atomic-commit fails with -EINVAL. The patch, gated on
# HYBRIS_HWC_DISABLE_COMPOSER_USAGE=1 (set in the launcher's compositor
# conf), strips the three usage bits QTI's IsUBwcEnabled() treats as UBWC
# triggers.
FILESEXTRAPATHS:prepend := "${THISDIR}/libhybris:"
SRC_URI:append:dace = " file://libhybris-a13-hooks.patch \
                          file://libhybris-dace-disable-ubwc-usage.patch"

# Bake the AOSP-13 library search path into libhybris's linker as the
# compile-time DEFAULT_HYBRIS_LD_LIBRARY_PATH. Without this, any daemon
# that dlopen()s Android libs (ngfd's droid-vibrator, bluebinder,
# sensorfwd's hybris adaptors, etc.) needs HYBRIS_LD_LIBRARY_PATH set
# in its own systemd-user unit -- brittle, easy to forget. Setting it
# here means every libhybris consumer uses the right path
# unconditionally; per-service overrides still work if needed.
EXTRA_OECONF:append:dace = " --with-default-hybris-ld-library-path=/var/lib/lxc/android/rootfs/system/lib:/system/lib:/vendor/lib:/usr/libexec/hal-droid/system/lib:/var/lib/lxc/android/rootfs/apex/com.android.runtime/lib/bionic"
