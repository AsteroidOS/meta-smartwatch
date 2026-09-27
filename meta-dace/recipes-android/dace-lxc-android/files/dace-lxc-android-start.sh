#!/bin/sh
# Set up the host-side mounts for the Halium-13 Android LXC container, then launch
# it. Mirrors UBPorts's start-android-container on the same hardware.

# Redirect stdout+stderr to /var/log/dace-lxc-android.log on the loop
# rootfs ext4 -- journald silently drops this service's output for reasons
# we haven't pinned down, and a file in the rootfs's persistent ext4 gives
# a guaranteed channel pullable from the ramdisk via /loop/...
exec >/var/log/dace-lxc-android.log 2>&1

echo "dace-lxc-android: =================== boot start ==================="
date 2>&1 || true

set -e

ROOTFS=/var/lib/lxc/android/rootfs

# Container root stays read-only. Do NOT overlay it (or /vendor): an overlayfs in
# the container mount tree makes Android-13 init SIGSEGV in early second stage.
# init tolerates the ro root (its "Read-only file system" warnings are non-fatal).
if ! mountpoint -q $ROOTFS; then
    echo "dace-lxc-android: $ROOTFS not mounted, aborting" >&2
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
        echo "dace-lxc-android: mounted binderfs"
    else
        echo "dace-lxc-android: binderfs mount failed" >&2
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
# Android sees dace's device-specific HALs. Note: NEVER bind
# /android/system over $ROOTFS/system -- the rootfs.img already has a
# working /system (Halium-13 build); dace's stock /system has a
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
VBP_OVERLAY=/run/dace-vendor-build.prop
if [ -f /android/vendor/build.prop ]; then
    sed -e 's/^vendor\.gralloc\.disable_ubwc=0/vendor.gralloc.disable_ubwc=1/' \
        /android/vendor/build.prop > "$VBP_OVERLAY"
    # Ensure the override added it even if the stock line is absent.
    grep -q '^vendor\.gralloc\.disable_ubwc=' "$VBP_OVERLAY" \
        || echo 'vendor.gralloc.disable_ubwc=1' >> "$VBP_OVERLAY"
    chmod 0644 "$VBP_OVERLAY"
    echo "dace-lxc-android: prepared vendor/build.prop overlay (disable_ubwc=1)"
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
    echo "dace-lxc-android: bound $src -> $dst"
    # After /android/vendor is bound RO into the container, layer our
    # disable_ubwc=1 build.prop on top of the stock one inside the container.
    if [ "$name" = "vendor" ] && [ -f "$VBP_OVERLAY" ] && [ -f "$dst/build.prop" ]; then
        mount --bind "$VBP_OVERLAY" "$dst/build.prop"
        echo "dace-lxc-android: overlaid vendor/build.prop (disable_ubwc=1)"
    fi
done

# ─ BT firmware (WCN3988) where the vendor HAL looks for it ──────────────
# The Qualcomm HAL opens /dev/ttyHS0, powers the chip and requests the firmware
# through the standard Android path (/vendor/firmware, /bt_firmware/image,
# /etc/firmware). Our dace-bt-firmware leaves it in the HOST's /lib/firmware/qca,
# which that path does not look at -> we mount a bind overlay: staging with the
# contents of the stock /vendor/firmware + the BT files.
# Which family is needed depends on the SOC the HAL picks (which it derives from
# the compatible of the DT BT node, see dace-boot-images.bb):
#   - soc=slate (stock, qcom,qcc5100) -> **slbtfw20.mbn + slnv20.bin** (sl*)
#     -> the one used today; without it: 'File Open Fail' and 'Controller Init
#        failed' (the chip answers Get Version but does not receive the patch).
#   - soc=cherokee (btattach/btqca path) -> apbtfw11.tlv + apnv11.bin (ap*)
# Both families are copied; they are ~320 KB in total.
BTFW_SRC=/lib/firmware/qca
if [ -d "$BTFW_SRC" ] && [ -d "$ROOTFS/vendor/firmware" ]; then
    VFW_STAGE=/run/dace-vendor-firmware
    rm -rf "$VFW_STAGE"
    mkdir -p "$VFW_STAGE"
    cp -a "$ROOTFS/vendor/firmware/." "$VFW_STAGE/" 2>/dev/null || true
    _n=0
    for _f in "$BTFW_SRC"/apbtfw*.tlv "$BTFW_SRC"/apnv*.bin \
              "$BTFW_SRC"/slbtfw*.mbn "$BTFW_SRC"/slbtfw*.tlv \
              "$BTFW_SRC"/slnv*.bin; do
        [ -e "$_f" ] || continue
        cp -a "$_f" "$VFW_STAGE/" 2>/dev/null && _n=$((_n + 1))
    done
    if mount --bind "$VFW_STAGE" "$ROOTFS/vendor/firmware"; then
        echo "dace-lxc-android: overlay /vendor/firmware (+$_n BT files)"
    fi
fi

# Mask vendor init's USB scripts. AsteroidOS owns the USB gadget end-to-end
# (init_gfs.service stages /config/usb_gadget/g1; dace-udc-bind binds UDC
# once adbd has its FunctionFS endpoints). Halium already neutralizes the
# system-side USB init (init.rc's `import init.usb*.rc` lines are commented
# out, usbd points to /system/bin/usbd_DISABLED, USB HALs are *_HYBRIS_DISABLED
# in init.disabled.rc), so the only remaining USB activity inside the
# container comes from Qualcomm vendor init scripts on the /vendor partition,
# which Android init auto-loads from /vendor/etc/init/ regardless of imports.
# Letting Qualcomm's init.qti.usb*.rc / init.usb.configfs.rc run rewrites g1's
# functions + configs/b.1 (the full QTI composite: ffs.adb + ffs.diag + mtp +
# ptp + mass_storage + accessory + qdss + uac2 + uvc) and binds the UDC --
# dace-udc-bind then sees ffs.adb removed, recreates an empty function, and
# the kernel refuses to bind UDC with ENODEV (no FunctionFS backing).
# Bind-mount an empty file over each so Android init parses nothing for them.
VENDOR_INIT="$ROOTFS/vendor/etc/init"
if [ -d "$VENDOR_INIT" ]; then
    echo "dace-lxc-android: vendor .rc files containing USB tokens (and their lines):"
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
        empty="/run/dace-mask-$(basename "$rc")"
        : > "$empty"
        if mount --bind "$empty" "$rc"; then
            echo "dace-lxc-android: masked vendor USB init: $(basename "$rc")"
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
    : > /run/dace-mask-pixelstats-vendor.rc
    if mount --bind /run/dace-mask-pixelstats-vendor.rc "$PIXELSTATS_RC"; then
        echo "dace-lxc-android: masked pixelstats-vendor.rc"
    fi
fi

# -- powerstateservice: add the 'interface' line to it ---------------
# Its .rc (vendor.qti.hardware.powerstateservice@1.0-service.rc) declares the
# service but WITHOUT 'interface', so hwservicemanager cannot map the
# `ctl.interface_start` that clients emit and the service NEVER starts:
#   init: Control message: Could not find
#     'vendor.qti.hardware.powerstateservice@1.0::IPowerStateService/default'
# (every ~60 s, forever). And it is the slate MCU's state PEER (TWM/deep
# sleep, /dev/power_state + /dev/slate_com_dev): without it, the MCU is left
# with nobody to answer it and the SoC ends in an Oops a few seconds after
# bringing up the link. The .rc is overlaid with the added line (same bind-mount
# as the USB/pixelstats ones) so init starts it on its own.
PSS_RC="$VENDOR_INIT/vendor.qti.hardware.powerstateservice@1.0-service.rc"
if [ -f "$PSS_RC" ] && ! mountpoint -q "$PSS_RC"; then
    if grep -qE '^[[:space:]]*interface[[:space:]]' "$PSS_RC"; then
        echo "dace-lxc-android: pss ya trae 'interface'"
    else
        awk '{ print } /^service[[:space:]]/ { print "    interface vendor.qti.hardware.powerstateservice@1.0::IPowerStateService default" }' \
            "$PSS_RC" > /run/dace-pss-service.rc 2>/dev/null
        if [ -s /run/dace-pss-service.rc ] && mount --bind /run/dace-pss-service.rc "$PSS_RC"; then
            echo "dace-lxc-android: powerstateservice .rc with 'interface' (init starts it on its own)"
        fi
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
    QTI_KERNEL_RC_OVERLAY=/run/dace-init.qti.kernel.rc
    sed -e '/boot_cdsp\/boot/d' -e '/cvp\/cvp\/boot/d' \
        "$QTI_KERNEL_RC" > "$QTI_KERNEL_RC_OVERLAY"
    chmod 0644 "$QTI_KERNEL_RC_OVERLAY"
    if mount --bind "$QTI_KERNEL_RC_OVERLAY" "$QTI_KERNEL_RC"; then
        echo "dace-lxc-android: masked CDSP/CVP boot writes in init.qti.kernel.rc"
    fi
fi

# Dump g1 state BEFORE container starts so we can compare against what
# dace-udc-bind sees later. init_gfs.service should have created g1 with
# ffs.adb; this lets us know whether anything is touching it before LXC.
echo "dace-lxc-android: /config/usb_gadget state BEFORE container start:"
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
    _tgt="$1"; _tmp="/run/dace-$(basename "$1")"
    [ -f "$_tgt" ] || { echo "dace-lxc-android: $_tgt missing, skip stub"; return 0; }
    mountpoint -q "$_tgt" && return 0
    cat > "$_tmp"
    mount --bind "$_tmp" "$_tgt" && echo "dace-lxc-android: stubbed $(basename "$_tgt")"
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

# system_dlkm (kernel modules for the Android container) -- mounted at
# $ROOTFS/system_dlkm. The dm device is created by dace-vendor-mount.service.
# (On the T5 it is a single device without a _b suffix, unlike the PW2.)
if [ -b /dev/mapper/system_dlkm ] && [ -d $ROOTFS/system_dlkm ]; then
    if ! mountpoint -q $ROOTFS/system_dlkm; then
        mount -o ro /dev/mapper/system_dlkm $ROOTFS/system_dlkm 2>/dev/null && \
            echo "dace-lxc-android: mounted /system_dlkm"
    fi
fi

# /metadata partition (Android-style). T5: p24 (p54 es init_boot).
if [ -b /dev/mmcblk0p24 ] && [ -d $ROOTFS/metadata ]; then
    if ! mountpoint -q $ROOTFS/metadata; then
        mount -o noatime,nosuid,nodev,discard /dev/mmcblk0p24 $ROOTFS/metadata && \
            echo "dace-lxc-android: mounted /metadata"
    fi
fi

# Vendor firmware (vfat partition mounted INSIDE vendor). T5: p14 = modem,
# which is a VFAT with image/ (modem.mdt + modem.b*) and verinfo.
if [ -b /dev/mmcblk0p14 ] && [ -d $ROOTFS/vendor/firmware_mnt ]; then
    if ! mountpoint -q $ROOTFS/vendor/firmware_mnt; then
        mount -t vfat -o ro,uid=1000,gid=1000,fmask=0337,dmask=0227 \
            /dev/mmcblk0p14 $ROOTFS/vendor/firmware_mnt 2>/dev/null && \
            echo "dace-lxc-android: mounted vendor firmware_mnt"
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
        echo "dace-lxc-android: mounted tmpfs /data (512M)"
    else
        echo "dace-lxc-android: WARNING /data tmpfs mount failed -> init_user0 will fail" >&2
    fi
fi

# WLAN: /data/vendor/wifi/sockets BEFORE launching the Android init.
# cnss-daemon (class late_start, init.qcom.rc) binds its "user socket" there;
# without it it dies with "Fail to bind user socket" and the WLAN cnss-genl
# bridge never comes up. The Android init does NOT create it (its post-fs-data
# does `mkdir /data/vendor` with encryption=Require and on a plain /data tmpfs
# without FBE it fails). We create it in the host namespace: the /data tmpfs is
# already mounted at $ROOTFS/data and the container inherits it, so init and
# cnss-daemon see it. Aurora pattern (which also mounts /data as tmpfs).
if mountpoint -q "$ROOTFS/data"; then
    mkdir -p "$ROOTFS/data/vendor/wifi/sockets" \
             "$ROOTFS/data/vendor/wifi/wpa/sockets" 2>/dev/null
    chown 1000:1000 "$ROOTFS/data/vendor" \
                    "$ROOTFS/data/vendor/wifi" \
                    "$ROOTFS/data/vendor/wifi/sockets" 2>/dev/null
    chmod 0771 "$ROOTFS/data/vendor" "$ROOTFS/data/vendor/wifi" 2>/dev/null
    chmod 0770 "$ROOTFS/data/vendor/wifi/sockets" 2>/dev/null
    echo "dace-lxc-android: preparado /data/vendor/wifi/sockets (WLAN)"
fi

# /mnt tmpfs + bind /persist into /mnt/vendor/persist
if ! mountpoint -q $ROOTFS/mnt; then
    mount -t tmpfs android_mnt $ROOTFS/mnt
    mkdir -p $ROOTFS/mnt/vendor/persist
fi
if mountpoint -q /persist && ! mountpoint -q $ROOTFS/mnt/vendor/persist; then
    mount --bind /persist $ROOTFS/mnt/vendor/persist
    echo "dace-lxc-android: bound /persist -> $ROOTFS/mnt/vendor/persist"
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
            echo "dace-lxc-android: bound $apex (from $(basename "$src"))"
            break
        fi
    done
done

# Launch second-stage init directly (env -i + PATH + INIT_STARTED_AT), as UBPorts
# does. A bare `/init` runs FirstStageMain inside the container -- wrong here (the
# host did first-stage's job) and its second-stage re-exec SIGSEGVs.
api_level=$(sed -n 's/^ro.build.version.sdk=//p' $ROOTFS/system/build.prop)
if [ "${api_level:-0}" -le 28 ]; then
    echo "dace-lxc-android: WARNING unexpected api_level='$api_level' (expected >=29 for second_stage launch)" >&2
fi

echo "dace-lxc-android: starting lxc container 'android' (api ${api_level:-?})..."
exec /usr/bin/lxc-start -n android -F -P /var/lib/lxc -- \
    /system/bin/env -i \
        PATH=/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/sbin:/system/sbin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin \
        INIT_STARTED_AT=0 \
        /init second_stage
