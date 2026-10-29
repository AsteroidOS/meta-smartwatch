#!/bin/sh
# Recreate the dm-linear mappings for the TicWatch Pro 5's (monaco/SW5100)
# stock Wear OS 13 partitions, which live inside the dynamic partition
# "super" (/dev/mmcblk0p7, 4 GiB), and mount them so the LXC container can
# bind them into its rootfs and libhybris can load the Android HALs.
#
# Tables read from the super's LP metadata with lpdump on a dump of the first
# 256 KiB of mmcblk0p7. The metadata geometry is at offset 4096 (NOT 0 --
# LP_PARTITION_RESERVED_BYTES), which is why an LP-magic check at offset 0
# fails. Every partition is a SINGLE extent (the Pixel Watch 2, aurora, had
# system_b fragmented in four pieces):
#
#   system       0 .. 2710904  linear super 2048       (1.29 GiB)
#   vendor       0 ..  580783  linear super 2712952    (283 MiB)
#   product      0 ..  705423  linear super 3293736    (344 MiB)
#   system_ext   0 ..  296631  linear super 3999160    (145 MiB)
#   vendor_dlkm  0 ..  124783  linear super 4295792
#   system_dlkm  0 ..     679  linear super 4420576
#
# The T5 is a single-slot device, so the devices have NO _b suffix.

set -e

SUPER="/dev/mmcblk0p7"

if [ ! -b "$SUPER" ]; then
    echo "dace-vendor-mount: $SUPER not present, aborting" >&2
    exit 1
fi

# Without LP metadata there is no Wear OS to map: it is not a fatal error (it
# allows booting on a T5 with an empty super).
# NOTE: compare with the BYTES ("gDla"), not with od -tx4 (which gives them
# reversed in little-endian: 616c4467, not 67446c61). With the bad comparison
# the script did exit 0 WITHOUT CREATING ANYTHING and the service looked OK
# (real bug, caught on the watch: /dev/mapper empty and /android unmounted
# after 'active (exited)').
if [ "$(dd if=$SUPER bs=1 skip=4096 count=4 2>/dev/null)" != "gDla" ]; then
    echo "dace-vendor-mount: $SUPER without LP metadata (geometry missing), nothing to do"
    exit 0
fi

dm_create_if_missing() {
    name=$1; shift
    table=$1
    if [ -b /dev/mapper/$name ]; then
        echo "dace-vendor-mount: /dev/mapper/$name already exists, skipping dm setup"
    else
        printf "%s" "$table" | dmsetup create $name
        echo "dace-vendor-mount: created /dev/mapper/$name"
    fi
}

dm_create_if_missing system      "0 2710904 linear $SUPER 2048"
dm_create_if_missing vendor      "0 580784 linear $SUPER 2712952"
dm_create_if_missing product     "0 705424 linear $SUPER 3293736"
dm_create_if_missing system_ext  "0 296632 linear $SUPER 3999160"
dm_create_if_missing vendor_dlkm "0 124784 linear $SUPER 4295792"
dm_create_if_missing system_dlkm "0 680 linear $SUPER 4420576"

mount_if_unmounted() {
    src=$1; dst=$2
    mkdir -p $dst
    if mountpoint -q $dst; then
        echo "dace-vendor-mount: $dst already mounted"
    else
        mount -o ro,nosuid,nodev $src $dst
        echo "dace-vendor-mount: $dst mounted from $src"
    fi
}

# /android/* is what the LXC container bind-mounts into its rootfs
# (dace-lxc-android-start.sh binds /android/vendor -> $ROOTFS/vendor).
mount_if_unmounted /dev/mapper/system      /android/system
mount_if_unmounted /dev/mapper/vendor      /android/vendor
mount_if_unmounted /dev/mapper/product     /android/product
mount_if_unmounted /dev/mapper/system_ext  /android/system_ext
mount_if_unmounted /dev/mapper/vendor_dlkm /android/vendor_dlkm
mount_if_unmounted /dev/mapper/system_dlkm /android/system_dlkm

# Compatibility symlinks: /vendor and /system are where Halium / libhybris
# expect to find the HAL .so's and the Android libs. Without them, every daemon
# (sensorfwd, ngfd-droid-vibrator, bluebinder, the launcher's hwcomposer QPA)
# would need its own HYBRIS_LD_LIBRARY_PATH.
#
#   /vendor -> /android/vendor (the T5 vendor_b ext4)
#   /system -> the CONTAINER's system (/var/lib/lxc/android/rootfs/system),
#              NOT /android/system: the T5 stock system is a raw ext4 mirror,
#              while the container's already has the AOSP layout with the apexes
#              resolved (libhardware.so, sensors*.so, ...).
#
# NOTE: the android-system package leaves /system -> /usr/libexec/hal-droid/system.
# If that symlink exists we replace it (that is why we check it first).
if [ -L /system ] && [ "$(readlink /system)" = "/usr/libexec/hal-droid/system" ]; then
    rm -f /system
fi
[ -L /vendor ] || { rm -rf /vendor 2>/dev/null; ln -sf /android/vendor /vendor; }
[ -L /system ] || { rm -rf /system 2>/dev/null; ln -sf /var/lib/lxc/android/rootfs/system /system; }

# ─ Modem/WLAN firmware (like aurora) ────────────────────────────────
# The modem subsystem firmware (modem.mdt + modem.b00..b29), the WLAN one
# (wlanmdsp.mbn) and the BDFs (bdwlan.*) live in /vendor/firmware_mnt/image, but
# firmware_class.path defaults to /vendor/firmware (which does not reach there).
# Without that path, remoteproc-mss does not find modem.mdt (ENOENT) and stays
# offline, which breaks WLAN (icnss waits for the modem WLFW) and BT (the QCA
# SoC init needs the modem blob). Aurora does it this way in its
# aurora-vendor-mount.sh.
if [ -b /dev/mmcblk0p14 ] && ! mountpoint -q /vendor/firmware_mnt 2>/dev/null; then
    mkdir -p /vendor/firmware_mnt 2>/dev/null || true
    mount -t vfat -o ro,uid=1000,gid=1000,fmask=0337,dmask=0227 \
        /dev/mmcblk0p14 /vendor/firmware_mnt 2>/dev/null || true
fi
if [ -f /vendor/firmware_mnt/image/modem.mdt ]; then
    echo /vendor/firmware_mnt/image > /sys/module/firmware_class/parameters/path && \
        echo "dace-vendor-mount: firmware_class.path -> /vendor/firmware_mnt/image"
fi
# This service runs very early (Before=local-fs.target); adsp_loader and the
# remoteproc nodes can appear later. Wait (max ~30 s) for them to exist so we do
# not skip the ADSP/modem start (it happened: adsp=mss=offline).
i=0
while [ $i -lt 150 ] && [ ! -e /sys/kernel/boot_adsp/boot ]; do
    sleep 0.2; i=$((i+1))
done
while [ $i -lt 150 ] && [ ! -e /sys/class/remoteproc/remoteproc0 ]; do
    sleep 0.2; i=$((i+1))
done
while [ $i -lt 150 ] && [ ! -e /sys/class/remoteproc/remoteproc1 ]; do
    sleep 0.2; i=$((i+1))
done

# ─ Enable RECOVERY on the remoteprocs (key for the modem DOG) ──────
# qcom_q6v5_pas hardcodes rproc->recovery_disabled = true (line 1047 of the
# source). With recovery disabled, a firmware fatal error (e.g. the modem DOG
# "stalled initialization", which fires at ~40 s) is NOT recovered: the kernel
# panics and the SoC resets ("rproc recovery state: disabled -> device crash").
# aurora LIVES WITH the modem crashes (its own note: "modem perpetually crashes
# & recovers"). By enabling recovery from sysfs, the same DOG becomes
# "recovery state: enabled and kick recovery process" and the watch survives;
# the modem relaunches itself. Verified 2026-09-19: crash #1/#2 every ~40 s with
# the watch stable and adb alive (before: immediate reset).
for r in /sys/class/remoteproc/remoteproc*; do
    [ -e "$r/recovery" ] || continue
    [ "$(cat "$r/recovery" 2>/dev/null)" = "enabled" ] && continue
    echo enabled > "$r/recovery" 2>/dev/null && \
        echo "dace-vendor-mount: $(basename $r) recovery -> enabled" > /dev/kmsg || true
done
# NOTE: the modem (remoteproc1) also ends up with recovery=enabled: the DOG of
# its firmware (if it is ever started for WLAN) does NOT reset the SoC; the
# driver relaunches it on its own (this was the 2026-09-19 fix).

# ─ Start the ADSP (the modem/WLAN fw waits for it to be alive) ──────────────
# The MODEM (remoteproc1) is NO LONGER started here. On dace the WLAN needs at
# least the order: qcacld/icnss2 chain loaded -> slate MCU UP (icnss2 uses
# qcom,is_slate_rfa and waits for the "slatefw" SSR) -> modem. Starting it that
# early (before the slate and the WLAN chain) only fed the crash-loop
# `DOG detects stalled initialization`. The modem start now lives in
# dace-wlan.sh (gated behind /etc/dace-kick-modem).
# The modem fw waits for the ADSP alive via tmr_slave2; without the ADSP the
# modem watchdog hangs with "DOG detects stalled initialization" and resets in a
# loop. NOTE: monaco_adsp_resource does NOT have .auto_boot, so the ADSP does not
# start on its own: one must write to /sys/kernel/boot_adsp/boot (sysfs created
# by adsp_loader_dlkm). The container does it too, but LATE.
if [ -e /sys/kernel/boot_adsp/boot ] && \
   [ "$(cat /sys/class/remoteproc/remoteproc0/state 2>/dev/null)" != "running" ]; then
    echo 1 > /sys/kernel/boot_adsp/boot 2>/dev/null || true
    i=0
    while [ $i -lt 100 ] && \
          [ "$(cat /sys/class/remoteproc/remoteproc0/state 2>/dev/null)" != "running" ]; do
        sleep 0.2; i=$((i+1))
    done
    echo "dace-vendor-mount: ADSP (remoteproc0) state=$(cat /sys/class/remoteproc/remoteproc0/state 2>/dev/null)" > /dev/kmsg || true
fi

echo "dace-vendor-mount: OK"
