SUMMARY = "LG Watch Urbane 2nd Ed — Nougat Android userspace for libhybris"
LICENSE = "CLOSED"

SRC_URI = "http://mowerk.net/misc/system-nougat-nemo.tar.gz \
           file://fw_bcmdhd.bin_a1 \
           file://fw_bcmdhd_apsta.bin_a1 \
           file://bcmdhd.cal_a1 \
           file://adsp.mdt \
           file://adsp.b00 \
           file://adsp.b01 \
           file://adsp.b02 \
           file://adsp.b03 \
           file://adsp.b04 \
           file://adsp.b05 \
           file://adsp.b06 \
           file://adsp.b07 \
           file://adsp.b08 \
           file://adsp.b10 \
           file://adsp.b11 \
           file://sensor_def_nemo.conf \
           file://sensors.msm8226.so \
           "
SRC_URI[md5sum] = "d70cab84ecb0c55eee99c334be9c6e9e"
SRC_URI[sha256sum] = "8fdae248e324fab5d6cb507de4d2790fc90575446110ed8067b3dd1fbe1fac16"
PV = "nougat"

COMPATIBLE_MACHINE = "nemo"
FILESEXTRAPATHS:prepend:nemo := "${THISDIR}/files:"

DEPENDS += "patchelf-native"

require recipes-android/android/android.inc

# Scaffolding overrides — keep all until the Nougat base is confirmed to
# provide each natively, then strip one at a time with a flash+verify cycle.
# Strip order: (1) build.prop sed (no-op for Nougat dump, remove cleanly),
# (2) sensor_def_nemo.conf, (3) sensors.msm8226.so, (4) ADSP firmware,
# (5) WiFi firmware.
# Note: lights.nemo.so and power.nemo.so are real files in the Nougat dump
# already named correctly — NO symlinks needed for those two.
do_install:append() {
    install -m 0644 ${UNPACKDIR}/fw_bcmdhd.bin_a1 ${D}/usr/libexec/hal-droid/system/vendor/firmware/fw_bcmdhd.bin_a1
    install -m 0644 ${UNPACKDIR}/fw_bcmdhd_apsta.bin_a1 ${D}/usr/libexec/hal-droid/system/vendor/firmware/fw_bcmdhd_apsta.bin_a1
    install -d ${D}/usr/libexec/hal-droid/system/etc/wifi/
    install -m 0644 ${UNPACKDIR}/bcmdhd.cal_a1 ${D}/usr/libexec/hal-droid/system/etc/wifi/bcmdhd.cal_a1
    install -m 0644 ${UNPACKDIR}/adsp.mdt ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.mdt
    install -m 0644 ${UNPACKDIR}/adsp.b00 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b00
    install -m 0644 ${UNPACKDIR}/adsp.b01 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b01
    install -m 0644 ${UNPACKDIR}/adsp.b02 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b02
    install -m 0644 ${UNPACKDIR}/adsp.b03 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b03
    install -m 0644 ${UNPACKDIR}/adsp.b04 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b04
    install -m 0644 ${UNPACKDIR}/adsp.b05 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b05
    install -m 0644 ${UNPACKDIR}/adsp.b06 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b06
    install -m 0644 ${UNPACKDIR}/adsp.b07 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b07
    install -m 0644 ${UNPACKDIR}/adsp.b08 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b08
    install -m 0644 ${UNPACKDIR}/adsp.b10 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b10
    install -m 0644 ${UNPACKDIR}/adsp.b11 ${D}/usr/libexec/hal-droid/system/vendor/firmware/adsp.b11
    install -d ${D}/usr/libexec/hal-droid/system/etc/sensors/
    install -m 0644 ${UNPACKDIR}/sensor_def_nemo.conf ${D}/usr/libexec/hal-droid/system/etc/sensors/sensor_def_nemo.conf
    install -m 0755 ${UNPACKDIR}/sensors.msm8226.so ${D}/usr/libexec/hal-droid/system/vendor/lib/hw/sensors.msm8226.so
    # Nougat dump already has nemo identity; sed is a no-op safety net kept
    # until the scaffolding strip pass confirms it.
    sed -i \
        -e 's/^ro.product.name=bass$/ro.product.name=nemo/' \
        -e 's/^ro.product.device=bass$/ro.product.device=nemo/' \
        -e 's/^ro.product.board=bass$/ro.product.board=nemo/' \
        ${D}/usr/libexec/hal-droid/system/build.prop
    # Relative symlinks so libhybris finds HALs by board name (nemo).
    # lights.nemo.so and power.nemo.so already exist as real binaries in the
    # Nougat dump and must not be replaced here.
    ln -sf hwcomposer.msm8226.so ${D}/usr/libexec/hal-droid/system/lib/hw/hwcomposer.nemo.so
    ln -sf gralloc.msm8226.so ${D}/usr/libexec/hal-droid/system/lib/hw/gralloc.nemo.so
    ln -sf memtrack.msm8226.so ${D}/usr/libexec/hal-droid/system/lib/hw/memtrack.nemo.so
    ln -sf sensors.msm8226.so ${D}/usr/libexec/hal-droid/system/vendor/lib/hw/sensors.nemo.so
    # The Nougat hwcomposer calls eglGpuPerfHintQCOM, which lives in
    # libEGL_adreno.so (vendor/lib/egl/) but is not in hwcomposer's DT_NEEDED
    # chain. The bionic linker resolves symbols before running constructors,
    # so libEGL.so's lazy load of libEGL_adreno.so is too late. Add it as an
    # explicit DT_NEEDED entry so it is loaded alongside libEGL.so.
    patchelf --add-needed libEGL_adreno.so \
        ${D}/usr/libexec/hal-droid/system/lib/hw/hwcomposer.msm8226.so
    # Symlink Adreno EGL libs into system/lib so the bionic linker finds them
    # (default search path is system/lib:vendor/lib, not vendor/lib/egl/).
    for lib in libEGL_adreno.so libGLESv1_CM_adreno.so libGLESv2_adreno.so \
               eglsubAndroid.so libplayback_adreno.so libq3dtools_adreno.so; do
        ln -sf ../vendor/lib/egl/${lib} ${D}/usr/libexec/hal-droid/system/lib/${lib}
    done
    # /system and /vendor at rootfs level so Android ELF binaries can find
    # their dynamic linker at /system/bin/linker and libraries at /system/lib.
    ln -sf usr/libexec/hal-droid/system ${D}/system
    ln -sf usr/libexec/hal-droid/system/vendor ${D}/vendor
}

FILES:android-system:append = " /system /vendor"
