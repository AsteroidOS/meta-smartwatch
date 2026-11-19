#!/bin/sh
# /usr/libexec/sol-sensors-hidl.sh
#
# Give sol a HIDL sensors HAL, because AsteroidOS's sensor client only speaks HIDL.
#
# sol's A14 vendor ships only the AIDL android.hardware.sensors-service.multihal.
# sensorfw's hybris plugin looks up android.hardware.sensors@{1.0,2.0,2.1}::ISensors
# on hwbinder; with nothing answering it loops on "HYBRIS CTL invalid sensor
# handle: -1" and mce's plugin loads time out. Swapping in the HIDL multihal is
# safe because only the front-end differs: hals.conf names the same sub-HAL,
# /vendor/lib/hw/sensors.usf.so, which exports sensorsHalGetSubHal_2_1.
#
# Binding over the AIDL binary's path means the two multihals can never run
# against the same sub-HAL at once.
#
# Two requirements:
#
#  1. SELinux label. A bind mount inherits the source label; from /run that is
#     u:object_r:tmpfs:s0, and Android init refuses to exec it even in
#     permissive mode ("has incorrect label or no domain transition"). /run has
#     no xattr support for chcon, so the binary goes on a dedicated tmpfs
#     mounted with a forced context, taken from vendor_file_contexts:
#         .../bin/hw/android\.hardware\.sensors@[0-9]\.[0-9]-service(\.multihal)?
#             u:object_r:hal_sensors_default_exec:s0
#
#  2. VINTF. hwservicemanager rejects HIDL registrations the vendor manifest
#     does not declare. registerAsService() then fails and the process SIGSEGVs
#     in its static destructors, which looks like a startup crash.
set -eu

# Act on /android/vendor (from sol-vendor-mount.service, ordered before us).
# /var/lib/lxc/android/rootfs/vendor is the same filesystem, but that bind is
# created by sol-lxc-android-start.sh, which runs after this unit.
VENDOR=/android/vendor
BIN_SRC=/usr/share/sol-sensors-hidl/android.hardware.sensors@2.1-service.multihal
FRAG_SRC=/usr/share/sol-sensors-hidl/android.hardware.sensors@2.1-multihal.xml
BIN_DST="$VENDOR/bin/hw/android.hardware.sensors-service.multihal"
VINTF_DIR="$VENDOR/etc/vintf/manifest"
CTX=/run/sol-sensors-hidl-ctx
LABEL=u:object_r:hal_sensors_default_exec:s0

# Fail the unit if these are missing, so wrong unit ordering is visible instead
# of sensors silently absent later.
[ -f "$BIN_SRC" ] || { echo "sol-sensors-hidl: $BIN_SRC missing -- recipe not installed?"; exit 1; }
[ -e "$BIN_DST" ] || { echo "sol-sensors-hidl: $BIN_DST absent -- container rootfs and/or vendor not mounted yet; check unit ordering"; exit 1; }

# --- 1. the binary, on a tmpfs whose files are force-labelled -----------------
if ! mountpoint -q "$CTX"; then
    mkdir -p "$CTX"
    mount -t tmpfs -o "context=$LABEL" sol-sensors-hidl "$CTX"
fi
cp "$BIN_SRC" "$CTX/multihal"
chmod 0755 "$CTX/multihal"

mountpoint -q "$BIN_DST" && umount "$BIN_DST"
mount --bind "$CTX/multihal" "$BIN_DST"
echo "sol-sensors-hidl: bound HIDL multihal ($(stat -c %s "$BIN_DST") bytes) over the AIDL path"

# --- 2. the VINTF fragment ----------------------------------------------------
# The manifest dir is read-only, so overlay it. Overwrite the AIDL fragment's
# content rather than adding a file, so exactly one sensors HAL is declared:
# a declared but absent AIDL service makes clients wait.
if [ -d "$VINTF_DIR" ]; then
    if ! mountpoint -q "$VINTF_DIR"; then
        mkdir -p /run/sol-vintf-upper /run/sol-vintf-work
        mount -t overlay sol-vintf \
            -o "lowerdir=$VINTF_DIR,upperdir=/run/sol-vintf-upper,workdir=/run/sol-vintf-work" \
            "$VINTF_DIR"
    fi
    cp "$FRAG_SRC" "$VINTF_DIR/android.hardware.sensors-multihal.xml"

    sync
    echo "sol-sensors-hidl: VINTF now declares the HIDL sensors HAL"
fi
