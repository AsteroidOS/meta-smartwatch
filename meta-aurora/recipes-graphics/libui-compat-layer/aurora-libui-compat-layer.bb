SUMMARY = "libhybris gralloc4 shim for aurora's Android-13 HAL"
DESCRIPTION = "Prebuilt libui_compat_layer.so. libhybris's gralloc \
abstraction only takes its gralloc4 path if android_dlopen() resolves \
graphic_buffer_allocator_allocate from this .so; otherwise it falls \
back to hw_get_module('gralloc') which on A13 hits the software stub \
and SIGSEGVs the launcher. Upstream libhybris doesn't build compat/ui, \
so we ship the shim. Built from libhybris compat/ui against A13 \
frameworks/native, NDK r25c sysroot, libc++ __1 ABI namespace. The \
mapper-side functions (lock/unlock/import/free) are stubbed -- not on \
the GPU compositor's allocate path."

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

COMPATIBLE_MACHINE = "aurora"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "https://dl.dropboxusercontent.com/scl/fi/i4uezyzcicp4vnaeyoqmr/libui_compat_layer.so?rlkey=19v19ml30yg8jic91boxnjskp&dl=1;downloadfilename=libui_compat_layer.so"
SRC_URI[md5sum] = "e1438d4d1393c1545a78a4d2ceb926fb"
S = "${UNPACKDIR}"

# Prebuilt bionic/armv7 .so loaded by libhybris's android linker (not glibc).
# Skip QA that assumes glibc/host-arch shared objects.
INSANE_SKIP:${PN} = "arch ldflags already-stripped textrel libdir staticdev dev-so file-rdeps"
SKIP_FILEDEPS = "1"
EXCLUDE_FROM_SHLIBS = "1"
PRIVATE_LIBS:${PN} = "libui.so libutils.so libcutils.so libhardware.so liblog.so libc++.so libm.so libdl.so libc.so"

# Install onto libhybris's android LD search path so android_dlopen() finds it.
do_install() {
    install -d ${D}/usr/libexec/hal-droid/system/lib
    install -m 0644 ${UNPACKDIR}/libui_compat_layer.so ${D}/usr/libexec/hal-droid/system/lib/libui_compat_layer.so
}

FILES:${PN} = "/usr/libexec/hal-droid/system/lib/libui_compat_layer.so"
