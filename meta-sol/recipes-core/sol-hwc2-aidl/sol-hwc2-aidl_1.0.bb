SUMMARY = "sol: AIDL-capable hwc2 compatibility layer for the Halium system"
DESCRIPTION = "libhwc2_compat_layer.so built with AidlComposerHal, plus the two AIDL stubs \
it links against, overlaid into /android/hybris/system/lib. \
\
sol's composer is AIDL composer3 on vndbinder. The libhwc2_compat_layer.so in \
the Halium 13 system image only knows the HIDL path, so libhybris finds no \
hwcomposer and asteroid-launcher aborts with 'failed to get hwcomposer service'. \
\
Built from Halium's libhybris (branch halium-13.0, plus source-patches/) in a Halium 14 tree with \
`lunch aosp_arm-ap2a-eng && m libhwc2_compat_layer`: build products of open \
source, not vendor blobs. hybris/include/hybris/hwc2/hwc2_compatibility_layer.h \
is identical between our libhybris and Halium's, so the ABI matches. \
\
A bridge: once sol has a Halium system built against its own A14 vendor, these \
files belong in that image and this recipe goes."

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

# Prebuilt 32-bit ARM Android shared objects: none of our toolchain QA applies.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"
EXCLUDE_FROM_SHLIBS = "1"
# libdir: the .so files sit under ${datadir} because nothing on the host links
# them; they are only overlaid into the Halium system's lib dir at runtime.
INSANE_SKIP:${PN} = "already-stripped arch ldflags dev-so file-rdeps textrel staticdev libdir"

inherit systemd

# The tarball's libhwc2_compat_layer.so was built with the two changes in
# source-patches/ applied to Halium's libhybris (halium-13.0, bf36ece). They
# are kept as the record of that build; BitBake does not apply them. The
# README.txt inside the tarball predates both and is out of date.
SRC_URI = "https://mowerk.net/asteroidos/sol/sol-hwc2-aidl-1.0.tar.gz \
           file://sol-hwc2-aidl-overlay.sh \
           file://sol-hwc2-aidl.service"
SRC_URI[sha256sum] = "cf0cf7e2ba81c137901d410205c501323bb305d9854397a5f9fb2ca906fbdf28"
SRC_URI[md5sum] = "703a54aef9165c79ac751e746ac1dc9a"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${datadir}/sol-hwc2-aidl
    install -m 0644 ${UNPACKDIR}/libhwc2_compat_layer.so \
        ${D}${datadir}/sol-hwc2-aidl/libhwc2_compat_layer.so
    install -m 0644 ${UNPACKDIR}/android.hardware.graphics.composer@2.4.so \
        ${D}${datadir}/sol-hwc2-aidl/android.hardware.graphics.composer@2.4.so
    install -m 0644 ${UNPACKDIR}/android.hardware.graphics.composer3-V4-ndk.so \
        ${D}${datadir}/sol-hwc2-aidl/android.hardware.graphics.composer3-V4-ndk.so
    install -m 0644 ${UNPACKDIR}/README.txt ${D}${datadir}/sol-hwc2-aidl/README.txt

    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-hwc2-aidl-overlay.sh \
        ${D}${libexecdir}/sol-hwc2-aidl-overlay.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-hwc2-aidl.service \
        ${D}${systemd_unitdir}/system/sol-hwc2-aidl.service
}

FILES:${PN} = "${datadir}/sol-hwc2-aidl \
               ${libexecdir}/sol-hwc2-aidl-overlay.sh \
               ${systemd_unitdir}/system/sol-hwc2-aidl.service"

RDEPENDS:${PN} = "sol-lxc-android"

SYSTEMD_SERVICE:${PN} = "sol-hwc2-aidl.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
