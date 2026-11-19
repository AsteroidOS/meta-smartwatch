#!/bin/sh
# Expose the Halium system's FLATTENED APEXes at /apex.
#
# /system/lib's libc.so, libm.so and libdl.so are symlinks into
# /apex/com.android.runtime/lib/bionic/, which apexd mounts on Android. We do
# not run apexd, so without this every Android library load fails with
# `library "libc.so" not found`. Halium ships flattened apexes, so a bind mount
# is enough.
set -e
# Follow the host's Halium system (see sol-vendor-mount), not the container
# rootfs: /apex must match the /system libhybris resolves to, or the bionic
# symlinks point at a different bionic than the libraries beside them.
if [ -d /android/hybris/system ]; then R=/android/hybris; else R=/var/lib/lxc/android/rootfs; fi

# Two image layouts: aurora's Halium-13 image keeps the flattened apexes under
# /system/apex with an empty /apex; our A14 image flattens them into /apex. With
# /apex empty every libhybris consumer (sensorfwd, ngfd, bluebinder) fails to
# load and boot hangs on psplash.
for apex in com.android.runtime com.android.art com.android.i18n; do
    dst="/apex/$apex"
    mountpoint -q "$dst" && continue
    # Layout A: flattened inside the image's own /apex.
    if [ -d "$R/apex/$apex" ]; then
        mkdir -p "$dst"
        mount --bind "$R/apex/$apex" "$dst"
        mount -o remount,ro,bind "$dst" 2>/dev/null || true
        echo "sol-android-apex: $dst <- image /apex/$apex"
        continue
    fi
    # Layout B: under /system/apex, possibly with a .release/.debug suffix.
    for suffix in .release .debug ""; do
        src="$R/system/apex/$apex$suffix"
        [ -d "$src" ] || continue
        mkdir -p "$dst"
        mount --bind "$src" "$dst"
        mount -o remount,ro,bind "$dst" 2>/dev/null || true
        echo "sol-android-apex: $dst <- system/apex/$(basename "$src")"
        break
    done
done

if [ -e /apex/com.android.runtime/lib/bionic/libc.so ]; then
    echo "sol-android-apex: bionic OK"
else
    echo "sol-android-apex: WARNING bionic missing -- Android libs will not load" >&2
fi
