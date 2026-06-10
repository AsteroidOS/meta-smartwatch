#!/bin/sh
# Recreate the dm-linear mappings for aurora's stock Android-13 partitions
# inside /super (/dev/mmcblk0p80, slot B) and mount them under /android/ so the
# LXC container can bind them into its rootfs.
#
# Tables observed from a running UBPorts install on this hardware (sudo dmsetup
# table). Hardcoded for slot B; re-derive if Google reflashes the watch's super
# layout.
#
#   system_b: multi-segment (4 fragments, total ~3.5 GB ext4)
#   vendor_b: single segment (~234 MB ext4)
#   vendor_dlkm_b: single segment (~60 MB ext4)

set -e

SUPER="/dev/mmcblk0p80"

if [ ! -b "$SUPER" ]; then
    echo "aurora-vendor-mount: $SUPER not present, aborting" >&2
    exit 1
fi

dm_create_if_missing() {
    name=$1; shift
    table=$1
    if [ -b /dev/mapper/$name ]; then
        echo "aurora-vendor-mount: /dev/mapper/$name already exists, skipping dm setup"
    else
        printf "%s" "$table" | dmsetup create $name
        echo "aurora-vendor-mount: created /dev/mapper/$name"
    fi
}

# vendor_b -- single segment
dm_create_if_missing vendor_b "0 467320 linear $SUPER 4898792"

# vendor_dlkm_b -- single segment
dm_create_if_missing vendor_dlkm_b "0 123952 linear $SUPER 5366112"

# system_b -- 4 fragments. dmsetup reads multi-line tables from stdin.
dm_create_if_missing system_b "\
0 3075904 linear $SUPER 1562192
3075904 1560144 linear $SUPER 2048
4636048 260016 linear $SUPER 4638776
4896064 2135184 linear $SUPER 5490064"

mount_if_unmounted() {
    src=$1; dst=$2
    mkdir -p $dst
    if mountpoint -q $dst; then
        echo "aurora-vendor-mount: $dst already mounted"
    else
        mount -o ro,nosuid,nodev $src $dst
        echo "aurora-vendor-mount: $dst mounted from $src"
    fi
}

mount_if_unmounted /dev/mapper/system_b       /android/system
mount_if_unmounted /dev/mapper/vendor_b       /android/vendor
mount_if_unmounted /dev/mapper/vendor_dlkm_b  /android/vendor_dlkm

# system_dlkm_b -- mounted at /system_dlkm in the container. Single segment.
dm_create_if_missing system_dlkm_b "0 680 linear $SUPER 4638096"

# Compatibility symlinks: /vendor and /system are where Halium / libhybris
# expect to find Android system/vendor libs and HAL .so's. Without these, every
# Halium-aware daemon (sensorfwd's libhybrissensorfw, ngfd's
# libngfd_droid-vibrator, bluebinder, etc.) needs a per-service
# HYBRIS_LD_LIBRARY_PATH drop-in.
#
# /vendor points at /android/vendor (vendor_b ext4 mounted above).  /system
# points at the LXC container's rootfs/system, NOT at /android/system:
# /android/system on the host is a half-populated linux-ish chroot mirror (the
# partition's bare ext4 contents), while the LXC bind exposes the AOSP layout
# with apex overlays already resolved (lib/libhardware.so,
# lib/android.hardware.sensors*.so, etc.).  libhybris's compiled-in /system/lib
# resolves to the right libs.
[ -L /vendor ] || { rm -rf /vendor 2>/dev/null; ln -sf /android/vendor /vendor; }
[ -L /system ] || { rm -rf /system 2>/dev/null; ln -sf /var/lib/lxc/android/rootfs/system /system; }

# cs40l26 haptic driver request_firmware()s cs40l26.wmfw etc. from
# /vendor/firmware. The cs40l26 module chain is loaded by initramfs init.sh
# (modules.load.aurora) where /vendor isn't visible yet, so probe silently
# fails and the i2c device is left unbound forever (no retry).  Re-bind now
# that /vendor exists; the driver re-probes and loads firmware.
if [ -d /sys/bus/i2c/drivers/cs40l26 ] && [ -d /sys/bus/i2c/devices/0-0043 ] \
        && [ ! -L /sys/bus/i2c/devices/0-0043/driver ]; then
    echo 0-0043 > /sys/bus/i2c/drivers/cs40l26/bind 2>/dev/null \
        && echo "aurora-vendor-mount: bound cs40l26 to 0-0043"
fi

# Modem subsystem firmware (modem.mdt + modem.b00..b29) lives under
# /vendor/firmware_mnt/image (a separate VFAT partition, /dev/mmcblk0p44). The
# kernel's firmware_class.path defaults to /vendor/firmware, which doesn't
# reach this dir, so remoteproc-mss fails its modem.mdt request_firmware with
# ENOENT and stays offline -- which breaks WLAN (icnss waits for WLFW on the
# modem) and BT (the QCA SoC initialization needs the modem-side firmware blob
# to load).
#
# We must mount the partition HERE, in the host namespace: the only other
# mount of mmcblk0p44 is done by aurora-lxc-android-start.sh into the
# container's rootfs/vendor/firmware_mnt -- a different path entirely, never
# visible at host /vendor/firmware_mnt. Without a host-side mount,
# firmware_class.path is never set (the block below is gated on modem.mdt
# visibility), every modem.* request_firmware fails with ENOENT, and the
# modem -- and with it WLAN and BT -- never comes up.
# Mount options mirror aurora-lxc-android-start.sh's firmware_mnt mount.
# The mkdir succeeds despite /vendor being read-only because firmware_mnt
# already exists as a mount-point directory on the stock vendor ext4.
if [ -b /dev/mmcblk0p44 ] && ! mountpoint -q /vendor/firmware_mnt 2>/dev/null; then
    mkdir -p /vendor/firmware_mnt 2>/dev/null || true
    mount -t vfat -o ro,uid=1000,gid=1000,fmask=0337,dmask=0227 \
            /dev/mmcblk0p44 /vendor/firmware_mnt 2>/dev/null \
        && echo "aurora-vendor-mount: mounted firmware_mnt (mmcblk0p44)" \
        || true
fi
if [ -f /vendor/firmware_mnt/image/modem.mdt ]; then
    echo /vendor/firmware_mnt/image > /sys/module/firmware_class/parameters/path
    echo "aurora-vendor-mount: firmware_class.path -> /vendor/firmware_mnt/image"
fi
# Kick the modem now that its firmware is reachable. Tolerate failure (set -e):
# if anything above didn't line up, the write is rejected and we'd otherwise
# exit 1 and fail the whole unit.
if [ -d /sys/class/remoteproc/remoteproc1 ] \
        && [ "$(cat /sys/class/remoteproc/remoteproc1/state 2>/dev/null)" = "offline" ]; then
    echo start > /sys/class/remoteproc/remoteproc1/state 2>/dev/null \
        && echo "aurora-vendor-mount: kicked remoteproc-mss (modem) boot" \
        || true
fi
