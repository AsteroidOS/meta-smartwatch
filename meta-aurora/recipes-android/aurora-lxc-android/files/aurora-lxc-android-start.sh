#!/bin/sh
# Set up the host-side mounts for the Halium-13 Android LXC container, then launch
# it. Mirrors UBPorts's start-android-container on the same hardware.

# Redirect stdout+stderr to /var/log/aurora-lxc-android.log on the loop
# rootfs ext4 -- journald silently drops this service's output for reasons
# we haven't pinned down, and a file in the rootfs's persistent ext4 gives
# a guaranteed channel pullable from the ramdisk via /loop/...
exec >/var/log/aurora-lxc-android.log 2>&1

echo "aurora-lxc-android: =================== boot start ==================="
date 2>&1 || true

set -e

ROOTFS=/var/lib/lxc/android/rootfs

# Container root stays read-only. Do NOT overlay it (or /vendor): an overlayfs in
# the container mount tree makes Android-13 init SIGSEGV in early second stage.
# init tolerates the ro root (its "Read-only file system" warnings are non-fatal).
if ! mountpoint -q $ROOTFS; then
    echo "aurora-lxc-android: $ROOTFS not mounted, aborting" >&2
    exit 1
fi

# Wipe the host-shared property area before each start: /dev/__properties__ is
# bind-mounted from the host and persists across runs; init (the property writer)
# SIGSEGVs if it re-mmaps a stale area left by a previous run.
rm -rf /dev/__properties__/* 2>/dev/null

# Clean ephemeral mounts left over from a previous container run. /apex, /data
# and /mnt are tmpfs we (re)create below, but the rootfs .mount keeps $ROOTFS
# mounted across container restarts, so a prior run's versions persist. A stale
# /apex (apexd's conscrypt/boringssl bind-mounts) makes the FIPS self-test fail
# -> `reboot,boringssl-self-check-failed` -> container reboot loop (only on a
# restart; a fresh boot has no prior state). Unmount them so the blocks below
# recreate them fresh -- same rationale as the property wipe above.
for _m in apex data mnt; do
    umount -R "$ROOTFS/$_m" 2>/dev/null || umount -l "$ROOTFS/$_m" 2>/dev/null || true
done

# Mount binderfs on the host (Android-13 IPC). bind into container via
# the lxc config's `lxc.mount.entry = /dev/binderfs dev/binderfs ...`.
mkdir -p /dev/binderfs
if ! mountpoint -q /dev/binderfs; then
    if mount -t binder binder /dev/binderfs 2>/dev/null; then
        echo "aurora-lxc-android: mounted binderfs"
    else
        echo "aurora-lxc-android: binderfs mount failed" >&2
    fi
fi

# Expose the binder nodes to the HOST too. The container gets /dev/{binder,
# hwbinder,vndbinder} via mount-binder.sh, but the host Qt/lipstick compositor
# (libhybris) also opens /dev/hwbinder to reach the container's HALs (gralloc
# allocator + composer over hwbinder). Without these host symlinks getService
# fails and the compositor never gets the HWC. Mirrors UBPorts's
# `ln -s /dev/binderfs/*binder /dev` on the host.
for _b in binder hwbinder vndbinder; do
    if [ -e "/dev/binderfs/$_b" ]; then
        ln -sf "/dev/binderfs/$_b" "/dev/$_b" 2>/dev/null || true
    fi
done

# Bind-mount stock /vendor (and /vendor_dlkm) INTO the container so
# Android sees aurora's device-specific HALs. Note: NEVER bind
# /android/system over $ROOTFS/system -- the rootfs.img already has a
# working /system (Halium-13 build); aurora's stock /system has a
# different layout that would break init.
#
# Overlay /vendor/build.prop with vendor.gralloc.disable_ubwc=1. The
# stock /vendor/build.prop has =0; Qualcomm's init.qcom.early_boot.sh is
# meant to flip it to 1 for Monaco/sw5100 (soc_hwid 517/486) but our LXC
# container doesn't run that script reliably. Without =1, QTI gralloc
# allocates UBWC buffers, and the only launcher-eligible SDE SSPP (plane
# 70, DMA pipe) doesn't scan UBWC -- atomic-commit fails with -EINVAL
# and the launcher loops on `prepare: validate failed`. Bind-mount a
# modified build.prop on top of the read-only stock one before container
# init parses it.
VBP_OVERLAY=/run/aurora-vendor-build.prop
if [ -f /android/vendor/build.prop ]; then
    sed -e 's/^vendor\.gralloc\.disable_ubwc=0/vendor.gralloc.disable_ubwc=1/' \
        /android/vendor/build.prop > "$VBP_OVERLAY"
    # Ensure the override added it even if the stock line is absent.
    grep -q '^vendor\.gralloc\.disable_ubwc=' "$VBP_OVERLAY" \
        || echo 'vendor.gralloc.disable_ubwc=1' >> "$VBP_OVERLAY"
    chmod 0644 "$VBP_OVERLAY"
    echo "aurora-lxc-android: prepared vendor/build.prop overlay (disable_ubwc=1)"
fi

for src in /android/vendor /android/vendor_dlkm; do
    name=$(basename "$src")
    dst="$ROOTFS/$name"
    if mountpoint -q "$dst"; then
        continue
    fi
    if [ ! -d "$dst" ]; then
        continue
    fi
    mount --bind "$src" "$dst"
    mount -o remount,ro,bind "$dst"
    echo "aurora-lxc-android: bound $src -> $dst"
    # After /android/vendor is bound RO into the container, layer our
    # disable_ubwc=1 build.prop on top of the stock one inside the container.
    if [ "$name" = "vendor" ] && [ -f "$VBP_OVERLAY" ] && [ -f "$dst/build.prop" ]; then
        mount --bind "$VBP_OVERLAY" "$dst/build.prop"
        echo "aurora-lxc-android: overlaid vendor/build.prop (disable_ubwc=1)"
    fi
done

# Mask vendor init's USB scripts. AsteroidOS owns the USB gadget end-to-end
# (init_gfs.service stages /config/usb_gadget/g1; aurora-udc-bind binds UDC
# once adbd has its FunctionFS endpoints). Halium already neutralizes the
# system-side USB init (init.rc's `import init.usb*.rc` lines are commented
# out, usbd points to /system/bin/usbd_DISABLED, USB HALs are *_HYBRIS_DISABLED
# in init.disabled.rc), so the only remaining USB activity inside the
# container comes from Qualcomm vendor init scripts on the /vendor partition,
# which Android init auto-loads from /vendor/etc/init/ regardless of imports.
# Letting Qualcomm's init.qti.usb*.rc / init.usb.configfs.rc run rewrites g1's
# functions + configs/b.1 (the full QTI composite: ffs.adb + ffs.diag + mtp +
# ptp + mass_storage + accessory + qdss + uac2 + uvc) and binds the UDC --
# aurora-udc-bind then sees ffs.adb removed, recreates an empty function, and
# the kernel refuses to bind UDC with ENODEV (no FunctionFS backing).
# Bind-mount an empty file over each so Android init parses nothing for them.
VENDOR_INIT="$ROOTFS/vendor/etc/init"
if [ -d "$VENDOR_INIT" ]; then
    echo "aurora-lxc-android: vendor .rc files containing USB tokens (and their lines):"
    for rc in "$VENDOR_INIT"/*.rc; do
        [ -f "$rc" ] || continue
        if grep -qiE 'usb|gadget|configfs|sys\.usb\.config' "$rc" 2>/dev/null; then
            echo "  $(basename "$rc"):"
            grep -niE 'usb|gadget|configfs|sys\.usb\.config' "$rc" 2>/dev/null | sed 's/^/    /'
        fi
    done
    # Only mask files whose NAME matches USB-gadget patterns (the HAL service
    # .rc files). Don't blanket-mask files that just contain a `usb` token in
    # a permission group declaration (e.g. dataqti.rc's `group ... usb ...`)
    # because masking them breaks unrelated QTI services.
    for rc in "$VENDOR_INIT"/*usb*.rc "$VENDOR_INIT"/*gadget*.rc "$VENDOR_INIT"/*configfs*.rc; do
        [ -f "$rc" ] || continue
        mountpoint -q "$rc" && continue
        empty="/run/aurora-mask-$(basename "$rc")"
        : > "$empty"
        if mount --bind "$empty" "$rc"; then
            echo "aurora-lxc-android: masked vendor USB init: $(basename "$rc")"
        fi
    done
fi

# Mask pixelstats-vendor.rc: pixelstats_vendor is Google's Pixel telemetry
# daemon. It polls for android.frameworks.stats.IStats/default at ~2 Hz
# forever, but IStats is implemented by the Android framework's statsd,
# which a Halium container doesn't run -- so every poll makes
# servicemanager fire a ctl.interface_start that init rejects with
# "Could not find 'aidl/android.frameworks.stats.IStats/default'",
# spamming kmsg for the whole uptime. Nothing consumes the telemetry
# here; mask the .rc like the USB ones above.
PIXELSTATS_RC="$VENDOR_INIT/pixelstats-vendor.rc"
if [ -f "$PIXELSTATS_RC" ] && ! mountpoint -q "$PIXELSTATS_RC"; then
    : > /run/aurora-mask-pixelstats-vendor.rc
    if mount --bind /run/aurora-mask-pixelstats-vendor.rc "$PIXELSTATS_RC"; then
        echo "aurora-lxc-android: masked pixelstats-vendor.rc"
    fi
fi

# Drop the CDSP and CVP remoteproc boot writes from init.qti.kernel.rc's
# early-boot action. The Compute-DSP (camera ML / NN / FastRPC compute offload)
# and CVP (Computer Vision Processor: camera EIS / motion / detection) only
# serve a camera + CV/ML use case -- the Pixel Watch 2 has no camera and
# AsteroidOS runs no CV/ML offload, so both are dead weight. Worse, each
# `init.qti.write.sh .../boot 1` blocks ~63s waiting for a remoteproc powerup /
# firmware that never completes, then fails -- pure startup churn. The ADSP boot
# (audio + the sensor pipeline) on the adjacent line is NOT touched. The file is
# on the read-only vendor partition, so overlay a sed'd copy that deletes just
# the two offending lines (same idiom as the build.prop / USB-rc masks above).
QTI_KERNEL_RC="$VENDOR_INIT/hw/init.qti.kernel.rc"
if [ -f "$QTI_KERNEL_RC" ] && ! mountpoint -q "$QTI_KERNEL_RC"; then
    QTI_KERNEL_RC_OVERLAY=/run/aurora-init.qti.kernel.rc
    sed -e '/boot_cdsp\/boot/d' -e '/cvp\/cvp\/boot/d' \
        "$QTI_KERNEL_RC" > "$QTI_KERNEL_RC_OVERLAY"
    chmod 0644 "$QTI_KERNEL_RC_OVERLAY"
    if mount --bind "$QTI_KERNEL_RC_OVERLAY" "$QTI_KERNEL_RC"; then
        echo "aurora-lxc-android: masked CDSP/CVP boot writes in init.qti.kernel.rc"
    fi
fi

# Dump g1 state BEFORE container starts so we can compare against what
# aurora-udc-bind sees later. init_gfs.service should have created g1 with
# ffs.adb; this lets us know whether anything is touching it before LXC.
echo "aurora-lxc-android: /config/usb_gadget state BEFORE container start:"
for g in /config/usb_gadget/*; do
    [ -d "$g" ] || continue
    echo "  $g UDC=$(cat "$g/UDC" 2>/dev/null)"
    [ -d "$g/functions" ] && echo "    functions: $(ls "$g/functions" 2>/dev/null | tr '\n' ' ')"
    for c in "$g"/configs/*; do
        [ -d "$c" ] || continue
        echo "    config $(basename "$c"): $(ls "$c" 2>/dev/null | tr '\n' ' ')"
    done
done

# All THREE servicemanagers (system servicemanager, hwservicemanager,
# vndservicemanager) must run the stock Android binary with
# libselinux_stubs.so LD_PRELOADed -- this is exactly what UBPorts does
# (their `mount` shows all three .rc files bind-mounted from system_b, each
# adding `setenv LD_PRELOAD /system/lib/libselinux_stubs.so`). The stub no-ops
# libselinux so the binaries neither abort on the missing selinuxfs (apparmor
# kernel = no selinuxfs) NOR fail-closed on their addService SELinux
# access-check. Without it:
#   - vndservicemanager aborts (selinux_status_open) -> its onrestart
#     class_restart hal cascades and crash-loops the display HALs;
#   - the servicemanagers' access-checks fail-closed, so vendor services can't
#     register: the Qualcomm composer never acquires `display.qservice` on
#     vndbinder (HWCSession::Init -> "Cannot initialize composer") and never
#     registers IComposer on hwbinder -> lipstick hangs in
#     getService(composer@2.1) / "failed to get hwcomposer service", no pixels.
# Bind-mount the verbatim UBPorts .rc for all three servicemanagers.
# libselinux_stubs.so ships at /system/lib/ in our UBPorts-derived
# android-rootfs.img.
stub_sm_rc() {
    _tgt="$1"; _tmp="/run/aurora-$(basename "$1")"
    [ -f "$_tgt" ] || { echo "aurora-lxc-android: $_tgt missing, skip stub"; return 0; }
    mountpoint -q "$_tgt" && return 0
    cat > "$_tmp"
    mount --bind "$_tmp" "$_tgt" && echo "aurora-lxc-android: stubbed $(basename "$_tgt")"
}

stub_sm_rc "$ROOTFS/vendor/etc/init/vndservicemanager.rc" <<'RC'
service vndservicemanager /vendor/bin/vndservicemanager /dev/vndbinder
    setenv LD_PRELOAD /system/lib/libselinux_stubs.so
    class core
    user system
    group system readproc
    task_profiles ServiceCapacityLow
    onrestart class_restart main
    onrestart class_restart hal
    onrestart class_restart early_hal
    shutdown critical
RC

stub_sm_rc "$ROOTFS/system/etc/init/hwservicemanager.rc" <<'RC'
service hwservicemanager /system/bin/hwservicemanager
    setenv LD_PRELOAD /system/lib/libselinux_stubs.so
    user system
    disabled
    group system readproc
    critical
    onrestart setprop hwservicemanager.ready false
    onrestart class_restart --only-enabled main
    onrestart class_restart --only-enabled hal
    onrestart class_restart --only-enabled early_hal
    task_profiles ServiceCapacityLow HighPerformance
    class animation
    shutdown critical
RC

stub_sm_rc "$ROOTFS/system/etc/init/servicemanager.rc" <<'RC'
service servicemanager /system/bin/servicemanager
    setenv LD_PRELOAD /system/lib/libselinux_stubs.so
    class core animation
    user system
    group system readproc
    critical
    onrestart restart apexd
    onrestart restart audioserver
    onrestart restart gatekeeperd
    onrestart class_restart --only-enabled main
    onrestart class_restart --only-enabled hal
    onrestart class_restart --only-enabled early_hal
    task_profiles ServiceCapacityLow
    shutdown critical
RC

# system_dlkm (kernel modules for the Android container) -- UBPorts
# mounts /dev/mapper/system_dlkm_b at $ROOTFS/system_dlkm. The
# dm device is created by aurora-vendor-mount.service.
if [ -b /dev/mapper/system_dlkm_b ] && [ -d $ROOTFS/system_dlkm ]; then
    if ! mountpoint -q $ROOTFS/system_dlkm; then
        mount -o ro /dev/mapper/system_dlkm_b $ROOTFS/system_dlkm 2>/dev/null && \
            echo "aurora-lxc-android: mounted /system_dlkm"
    fi
fi

# /metadata partition (Android-style)
if [ -b /dev/mmcblk0p54 ] && [ -d $ROOTFS/metadata ]; then
    if ! mountpoint -q $ROOTFS/metadata; then
        mount -o noatime,nosuid,nodev,discard /dev/mmcblk0p54 $ROOTFS/metadata && \
            echo "aurora-lxc-android: mounted /metadata"
    fi
fi

# Vendor firmware (vfat partition at p44, mounted INSIDE vendor)
if [ -b /dev/mmcblk0p44 ] && [ -d $ROOTFS/vendor/firmware_mnt ]; then
    if ! mountpoint -q $ROOTFS/vendor/firmware_mnt; then
        mount -t vfat -o ro,uid=1000,gid=1000,fmask=0337,dmask=0227 \
            /dev/mmcblk0p44 $ROOTFS/vendor/firmware_mnt 2>/dev/null && \
            echo "aurora-lxc-android: mounted vendor firmware_mnt"
    fi
fi

# /data: vold/init_user0 need a writable fs here. tmpfs is ephemeral but
# fine for Halium-style use (HALs only, no persistent app data); keeps
# image headroom small and avoids needing mkfs. system:system 0771 to
# match Android's /data layout. A persistent /data on the userdata
# partition can replace this when needed.
if [ -d "$ROOTFS/data" ] && ! mountpoint -q "$ROOTFS/data"; then
    if mount -t tmpfs -o rw,nosuid,nodev,mode=0771,size=512M android_data "$ROOTFS/data"; then
        chown 1000:1000 "$ROOTFS/data" 2>/dev/null
        echo "aurora-lxc-android: mounted tmpfs /data (512M)"
    else
        echo "aurora-lxc-android: WARNING /data tmpfs mount failed -> init_user0 will fail" >&2
    fi
fi

# /mnt tmpfs + bind /persist into /mnt/vendor/persist
if ! mountpoint -q $ROOTFS/mnt; then
    mount -t tmpfs android_mnt $ROOTFS/mnt
    mkdir -p $ROOTFS/mnt/vendor/persist
fi
if mountpoint -q /persist && ! mountpoint -q $ROOTFS/mnt/vendor/persist; then
    mount --bind /persist $ROOTFS/mnt/vendor/persist
    echo "aurora-lxc-android: bound /persist -> $ROOTFS/mnt/vendor/persist"
fi

# /apex: bind the flattened runtime/art/i18n apexes baked into the rootfs (apexd
# mounts the rest inside the container), matching UBPorts.
if ! mountpoint -q $ROOTFS/apex; then
    mount -t tmpfs -o mode=755 android_apex $ROOTFS/apex
fi
for apex in com.android.runtime com.android.art com.android.i18n; do
    dst=$ROOTFS/apex/$apex
    if mountpoint -q "$dst"; then
        continue
    fi
    # Halium may ship the apex as a bare dir or with a .release/.debug
    # suffix; pick whichever exists (matches UBPorts mount-android-partitions).
    for suffix in .release .debug ""; do
        src=$ROOTFS/system/apex/$apex$suffix
        if [ -d "$src" ]; then
            mkdir -p "$dst"
            mount --bind "$src" "$dst"
            mount -o remount,ro,bind "$dst"
            echo "aurora-lxc-android: bound $apex (from $(basename "$src"))"
            break
        fi
    done
done

# Launch second-stage init directly (env -i + PATH + INIT_STARTED_AT), as UBPorts
# does. A bare `/init` runs FirstStageMain inside the container -- wrong here (the
# host did first-stage's job) and its second-stage re-exec SIGSEGVs.
api_level=$(sed -n 's/^ro.build.version.sdk=//p' $ROOTFS/system/build.prop)
if [ "${api_level:-0}" -le 28 ]; then
    echo "aurora-lxc-android: WARNING unexpected api_level='$api_level' (expected >=29 for second_stage launch)" >&2
fi

echo "aurora-lxc-android: starting lxc container 'android' (api ${api_level:-?})..."
exec /usr/bin/lxc-start -n android -F -P /var/lib/lxc -- \
    /system/bin/env -i \
        PATH=/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/sbin:/system/sbin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin \
        INIT_STARTED_AT=0 \
        /init second_stage
