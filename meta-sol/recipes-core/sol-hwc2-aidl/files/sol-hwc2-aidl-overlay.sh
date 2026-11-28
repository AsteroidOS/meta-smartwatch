#!/bin/sh
# /usr/libexec/sol-hwc2-aidl-overlay.sh
#
# Put sol's AIDL-capable libhwc2_compat_layer.so, and the two AIDL stubs it
# needs, into the Halium system's library directory.
#
# The image's own compat layer is HIDL-only, while sol's composer is AIDL
# composer3 (IComposer/default on vndbinder). Without this, asteroid-launcher
# aborts with "failed to get hwcomposer service".
#
# An overlay, because /android/hybris/system/lib is a read-only loop mount on a
# write-protected loop device, and two of the files (composer@2.4,
# composer3-V4-ndk) do not exist there to bind over. hybris-system.img is
# aurora's shared Halium-13 image; once sol has its own Halium system these
# files belong inside it and this script goes.
set -eu

LIBDIR=/android/hybris/system/lib
SRCDIR=/usr/share/sol-hwc2-aidl
UPPER=/run/sol-hwc2-upper
WORK=/run/sol-hwc2-work

[ -d "$LIBDIR" ] || { echo "sol-hwc2-aidl: $LIBDIR absent, hybris system not mounted"; exit 0; }

if mountpoint -q "$LIBDIR" && [ -e "$LIBDIR/android.hardware.graphics.composer3-V4-ndk.so" ]; then
    echo "sol-hwc2-aidl: overlay already active"
    exit 0
fi

mkdir -p "$UPPER" "$WORK"
mount -t overlay sol-hwc2 -o "lowerdir=$LIBDIR,upperdir=$UPPER,workdir=$WORK" "$LIBDIR"

for f in libhwc2_compat_layer.so \
         android.hardware.graphics.composer@2.4.so \
         android.hardware.graphics.composer3-V4-ndk.so; do
    cp "$SRCDIR/$f" "$LIBDIR/$f"
    chmod 0644 "$LIBDIR/$f"
done
sync

echo "sol-hwc2-aidl: overlay active, libhwc2_compat_layer.so is $(stat -c %s "$LIBDIR/libhwc2_compat_layer.so") bytes"
