SUMMARY = "LG Watch Urbane 2nd Ed -- Nougat Android userspace for libhybris"
LICENSE = "CLOSED"

SRC_URI = "http://mowerk.net/misc/system-nougat-nemo.tar.gz"
SRC_URI[md5sum] = "d70cab84ecb0c55eee99c334be9c6e9e"
SRC_URI[sha256sum] = "8fdae248e324fab5d6cb507de4d2790fc90575446110ed8067b3dd1fbe1fac16"
PV = "nougat"

COMPATIBLE_MACHINE = "nemo"

DEPENDS += "patchelf-native"

require recipes-android/android/android.inc

do_install:append() {
    # Relative symlinks so libhybris finds HALs by board name (nemo).
    # lights.nemo.so and power.nemo.so already exist as real binaries in the
    # Nougat dump; do not replace them with symlinks here.
    ln -sf hwcomposer.msm8226.so ${D}/usr/libexec/hal-droid/system/lib/hw/hwcomposer.nemo.so
    ln -sf gralloc.msm8226.so ${D}/usr/libexec/hal-droid/system/lib/hw/gralloc.nemo.so
    ln -sf memtrack.msm8226.so ${D}/usr/libexec/hal-droid/system/lib/hw/memtrack.nemo.so
    ln -sf sensors.msm8226.so ${D}/usr/libexec/hal-droid/system/vendor/lib/hw/sensors.nemo.so
    # hwcomposer.msm8226.so calls eglGpuPerfHintQCOM, which lives in
    # libEGL_adreno.so (vendor/lib/egl/) but is absent from hwcomposer's
    # DT_NEEDED chain. The bionic linker resolves symbols before constructors
    # run, so libEGL.so's lazy load of libEGL_adreno.so is too late. Add it
    # as an explicit DT_NEEDED entry so it is loaded alongside libEGL.so.
    patchelf --add-needed libEGL_adreno.so \
        ${D}/usr/libexec/hal-droid/system/lib/hw/hwcomposer.msm8226.so
    # Symlink Adreno EGL libs into system/lib so the bionic linker finds them;
    # its default search path is system/lib:vendor/lib, not vendor/lib/egl/.
    for lib in libEGL_adreno.so libGLESv1_CM_adreno.so libGLESv2_adreno.so \
               eglsubAndroid.so libplayback_adreno.so libq3dtools_adreno.so; do
        ln -sf ../vendor/lib/egl/${lib} ${D}/usr/libexec/hal-droid/system/lib/${lib}
    done
    # /system and /vendor at rootfs root so Android ELF binaries resolve their
    # dynamic linker at /system/bin/linker and libraries at /system/lib.
    ln -sf usr/libexec/hal-droid/system ${D}/system
    ln -sf usr/libexec/hal-droid/system/vendor ${D}/vendor
}

FILES:android-system:append = " /system /vendor"
