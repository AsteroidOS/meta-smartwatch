inherit gettext

SUMMARY = "Downloads the Ticwatch E & S /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/ubcsdhlbv09jxxo/system-mooneye.tar.gz \
           file://60-mtk-audio.rules \
          "
SRC_URI[md5sum] = "c1d225bca2257d13724600b3edac0e44"
SRC_URI[sha256sum] = "9c89cbbd9ececff94467912498b19b239a6b8f6a3a72927cf75d0400e391ac99"
PV = "oreo"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "mooneye"
INSANE_SKIP:${PN} = "already-stripped"
S = "${UNPACKDIR}"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    install -d ${D}/system/
    cp -r system/* ${D}/system/

    install -d ${D}/usr/
    cp -r usr/* ${D}/usr/

    install -d ${D}${includedir}/android
    cp -r include/* ${D}${includedir}/android/

    install -d ${D}${libdir}/pkgconfig
    install -m 0644 ${D}${includedir}/android/android-headers.pc ${D}${libdir}/pkgconfig
    rm ${D}${includedir}/android/android-headers.pc

    cd ${D}
    ln -s system/vendor vendor

    patch_audio_hal ${D}/system/vendor/lib/hw/audio.primary.mt2601.so

    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/60-mtk-audio.rules ${D}${sysconfdir}/udev/rules.d/
}

# The audio HAL creates its Bluetooth call (HFP) controller while it opens,
# and that controller looks up Android's "power" service for wakelocks.
# Without a binder context manager, defaultServiceManager() waits forever, so
# pulseaudio never gets a sink. servicemanager can't run without SELinux.
# Make the lookup helper return at once, as it does when the service is
# missing; every caller already handles a missing power manager.
AUDIO_HAL_SHA256 = "4a89d1d00e808bd8ae6bb2ad48120e9863a7ec1aee26f1c101fffbda1969bedc"
AUDIO_HAL_PATCHED_SHA256 = "96a0ca5fba44a3535266dc1479938a643e255bf2e7b8b5b2575987eba58a8b70"
patch_audio_hal() {
    hal="$1"
    if [ "$(sha256sum "$hal" | cut -d' ' -f1)" != "${AUDIO_HAL_SHA256}" ]; then
        bbfatal "$hal is not the known audio HAL, can't patch it"
    fi
    # At file offset 0x5f700 (390912): mov r0, #0; bx lr
    printf '\000\000\240\343\036\377\057\341' > ${WORKDIR}/hal-patch.bin
    dd if=${WORKDIR}/hal-patch.bin of="$hal" bs=1 seek=390912 conv=notrunc status=none
    if [ "$(sha256sum "$hal" | cut -d' ' -f1)" != "${AUDIO_HAL_PATCHED_SHA256}" ]; then
        bbfatal "patching $hal failed"
    fi
}

do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/system /vendor /usr ${sysconfdir}/udev"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
