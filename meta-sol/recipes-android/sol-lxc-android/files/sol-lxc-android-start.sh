#!/bin/sh
# Host-side setup for sol's Halium Android container, then launch it.
# Adapted from meta-aurora's aurora-lxc-android-start.sh; differences from
# aurora are marked SOL.

exec >/var/log/sol-lxc-android.log 2>&1
echo "sol-lxc-android: =================== boot start ==================="
date 2>&1 || true
set -e

ROOTFS=/var/lib/lxc/android/rootfs

if ! mountpoint -q $ROOTFS; then
    echo "sol-lxc-android: $ROOTFS not mounted, aborting" >&2
    exit 1
fi

# The container root stays read-only. Do NOT overlay it: an overlayfs in the
# container mount tree makes Android init SIGSEGV in early second stage.

# /dev/__properties__ is bind-mounted from the host and persists across runs;
# init (the property writer) SIGSEGVs if it re-mmaps a stale area.
rm -rf /dev/__properties__/* 2>/dev/null || true
mkdir -p /dev/__properties__ /dev/socket

# Clean ephemeral mounts from a previous run. The rootfs mount survives container
# restarts, so a stale /apex (apexd's bind mounts) makes the FIPS self-test fail
# and the container reboot-loops.
for _m in apex data mnt; do
    umount -R "$ROOTFS/$_m" 2>/dev/null || umount -l "$ROOTFS/$_m" 2>/dev/null || true
done

# binderfs on the host; bound into the container via the lxc config.
mkdir -p /dev/binderfs
if ! mountpoint -q /dev/binderfs; then
    mount -t binder binder /dev/binderfs 2>/dev/null \
        && echo "sol-lxc-android: mounted binderfs" \
        || echo "sol-lxc-android: binderfs mount failed" >&2
fi

# Expose the binder nodes to the HOST too: libhybris in asteroid-launcher opens
# /dev/hwbinder to reach the container's composer and allocator HALs. Without
# these symlinks getService() fails and there are no pixels.
for _b in binder hwbinder vndbinder; do
    [ -e "/dev/binderfs/$_b" ] && ln -sf "/dev/binderfs/$_b" "/dev/$_b" 2>/dev/null || true
done

# Bind sol's A14 vendor partitions into the container. Its own /system is the
# Halium one that libhybris needs; never bind /android/system over it.
# system_ext and product: init compiles the sepolicy CIL at boot and the vendor
# policy references types that system_ext declares, so secilc fails without them.
# odm breaks a symlink loop (image /odm/etc -> /vendor/odm/etc, vendor
# /vendor/odm -> /odm). ELOOP on the SKU manifest aborts the whole VINTF device
# manifest fetch and servicemanager rejects every vendor HAL; with the real odm
# bound the lookup is a plain ENOENT, which libvintf tolerates.
for src in /android/vendor /android/vendor_dlkm /android/system_ext /android/product /android/odm; do
    name=$(basename "$src")
    dst="$ROOTFS/$name"
    mountpoint -q "$dst" && continue
    [ -d "$dst" ] || continue
    mount --bind "$src" "$dst"
    mount -o remount,ro,bind "$dst"
    echo "sol-lxc-android: bound $src -> $dst"
done

# SOL: slot A, so the dm names end _a (aurora is _b). Created by
# sol-vendor-mount.service.
if [ -b /dev/mapper/system_dlkm_a ] && [ -d $ROOTFS/system_dlkm ]; then
    mountpoint -q $ROOTFS/system_dlkm || \
        mount -o ro /dev/mapper/system_dlkm_a $ROOTFS/system_dlkm 2>/dev/null \
            && echo "sol-lxc-android: mounted /system_dlkm"
fi

# SOL: metadata is mmcblk0p54.
if [ -b /dev/mmcblk0p54 ] && [ -d $ROOTFS/metadata ]; then
    mountpoint -q $ROOTFS/metadata || \
        mount -o noatime,nosuid,nodev,discard /dev/mmcblk0p54 $ROOTFS/metadata 2>/dev/null \
            && echo "sol-lxc-android: mounted /metadata"
fi

# SOL: the modem partition (mmcblk0p43, WLAN/BT firmware) is mounted host-side
# at /vendor/firmware_mnt and reaches the container through the vendor bind.

# Mask the vendor's USB init scripts: usb-moded and init_gfs own the gadget.
# The hw/ glob matters: hw/init.swp5100.usb.rc (init.qcom.rc -> init.target.rc
# -> init.sw5100.rc) rewrites g1/UDC repeatedly and removes g1's function links
# on the gadget the host has already bound, and that resets the SoC.
# Qualcomm's BT HAL is masked too: it does not drive sol's Broadcom chip and
# would race the AIDL pixel_watch HAL that bluebinder proxies (see sol.conf).
# The health HAL and pixelstats are masked as on aurora. Both read kernel
# uevents from a netlink socket but, with no Android framework to report to,
# stop draining it after some hours. The health HAL's socket is registered with
# EPOLLWAKEUP, so its full queue keeps the NETLINK wakeup source active and the
# watch never suspends (seen after 27 h uptime: Rmem full, 7450 drops).
# AsteroidOS reads the battery through power_supply, not the health HAL.
VENDOR_INIT="$ROOTFS/vendor/etc/init"
if [ -d "$VENDOR_INIT" ]; then
    for rc in "$VENDOR_INIT"/*bluetooth@1.0-service-qti*.rc \
              "$VENDOR_INIT"/android.hardware.health-service.eos.rc "$VENDOR_INIT"/pixelstats-vendor.rc \
              "$VENDOR_INIT"/*usb*.rc "$VENDOR_INIT"/*gadget*.rc "$VENDOR_INIT"/*configfs*.rc \
              "$VENDOR_INIT"/hw/*usb*.rc "$VENDOR_INIT"/hw/*gadget*.rc "$VENDOR_INIT"/hw/*configfs*.rc; do
        [ -f "$rc" ] || continue
        mountpoint -q "$rc" && continue
        empty="/run/sol-mask-$(basename "$rc")"
        : > "$empty"
        mount --bind "$empty" "$rc" && echo "sol-lxc-android: masked $(basename "$rc")"
    done
fi

# Hand the Bluetooth rfkill switch to the HAL. Android's init gives
# /sys/class/rfkill/rfkill0 to the bluetooth user by NUMBER, which is right on
# stock, where the chip registers first. Here brcmfmac registers before nitrous,
# so rfkill0 is a Wi-Fi switch and the HAL aborts with "Unable to open rfkill
# state" (vendor error 145) on every initialize. Match the switch by name.
for r in /sys/class/rfkill/rfkill*; do
    [ "$(cat "$r/name" 2>/dev/null)" = nitrous_bluetooth ] || continue
    chown 1002:3002 "$r/state" "$r/type" && chmod 0660 "$r/state" \
        && echo "sol-lxc-android: $(basename "$r") (nitrous_bluetooth) handed to the bluetooth HAL"
done

# Replace an rc file with the heredoc on stdin, through a bind mount from /run.
stub_sm_rc() {
    _tgt="$1"; _tmp="/run/sol-$(basename "$1")"
    [ -f "$_tgt" ] || { echo "sol-lxc-android: $_tgt missing, skip stub"; return 0; }
    mountpoint -q "$_tgt" && return 0
    cat > "$_tmp"
    mount --bind "$_tmp" "$_tgt" && echo "sol-lxc-android: stubbed $(basename "$_tgt")"
}

# vndservicemanager: no LD_PRELOAD (see servicemanager below). If it dies,
# nothing claims /dev/vndbinder and the composer loops on "Waiting 1s on context
# object" without registering composer3, while looking healthy in ps.
stub_sm_rc "$ROOTFS/vendor/etc/init/vndservicemanager.rc" <<'RC'
service vndservicemanager /vendor/bin/vndservicemanager /dev/vndbinder
    class core
    user system
    group system readproc
    onrestart class_restart main
    onrestart class_restart hal
    onrestart class_restart early_hal
    shutdown critical
RC

# hwservicemanager: A14 moved it to /system_ext. Stubbed without `critical`, so
# an abort no longer makes init reboot the container after 4 restarts, and
# without `disabled`, because nothing here starts it on demand. sol's TEE chain
# is HIDL @1.0 (qseecom, qteeconnector, strongbox keymint): without
# hwservicemanager keymint never registers and keystore2's restart storm stalls
# the container.
stub_sm_rc "$ROOTFS/system_ext/etc/init/hwservicemanager.rc" <<'RC'
service hwservicemanager /system/system_ext/bin/hwservicemanager
    user system
    group system readproc
    onrestart setprop hwservicemanager.ready false
    class core
RC

# servicemanager: no LD_PRELOAD and not critical. libselinux_stubs.so was for a
# kernel without selinuxfs; selinuxfs is real now (permissive), and stubbing
# libselinux over a genuine context is the suspected cause of its SIGABRT.
# Without `critical` a crash stays visible instead of rebooting the container.
stub_sm_rc "$ROOTFS/system/etc/init/servicemanager.rc" <<'RC'
service servicemanager /system/bin/servicemanager
    class core animation
    user system
    group system readproc
    onrestart restart apexd
    onrestart class_restart --only-enabled main
    onrestart class_restart --only-enabled hal
    onrestart class_restart --only-enabled early_hal
    shutdown critical
RC

# surfaceflinger: never started. composer3 allows one client and surfaceflinger
# takes it, so asteroid-launcher's createClient fails with EX_SERVICE_SPECIFIC
# 6. The launcher is the compositor here. `disabled` alone is not enough: a
# `restart surfaceflinger` in another onrestart still starts it, and the stock
# class `core animation` is started by init.rc, so the class goes too.
stub_sm_rc "$ROOTFS/system/etc/init/surfaceflinger.rc" <<'RC'
service surfaceflinger /system/bin/surfaceflinger
    class sol_never_started
    user system
    group graphics drmrpc readproc
    capabilities SYS_NICE
    disabled
RC

# Display allocator, stock except for `onrestart restart surfaceflinger`, which
# would bring surfaceflinger back to take the composer client.
stub_sm_rc "$ROOTFS/vendor/etc/init/vendor.qti.hardware.display.allocator-service.rc" <<'RC'
service vendor.qti.hardware.display.allocator /vendor/bin/hw/vendor.qti.hardware.display.allocator-service
    class hal animation
    user system
    group graphics drmrpc
    capabilities SYS_NICE
RC



# zygote: not started. With no framework it aborts on an empty BOOTCLASSPATH
# and init restarts it every ~5 s, each time taking the zygote_kwl wakelock and
# restarting audioserver. The class and the onrestart lines go.
stub_sm_rc "$ROOTFS/system/etc/init/hw/init.zygote32.rc" <<'RC'
service zygote /system/bin/app_process -Xzygote /system/bin --zygote --start-system-server
    class sol_never_started
    priority -20
    user root
    group root readproc reserved_disk
    disabled
RC

# audioserver: not started; PulseAudio owns audio. module-droid-card loads
# audio.primary in-process, and with audioserver as a second owner of the HAL
# and the AudioReach graph, PA blocks in futex_wait for good. aurora does not run
# it either. The onrestart lines go too: they restart vendor.audio-hal.
stub_sm_rc "$ROOTFS/system/etc/init/audioserver.rc" <<'RC'
service audioserver /system/bin/audioserver
    class sol_never_started
    user audioserver
    group audio camera drmrpc media mediadrm net_bt net_bt_admin net_bw_acct wakelock
    capabilities BLOCK_SUSPEND
    rlimit rtprio 10 10
    ioprio rt 4
    task_profiles ProcessCapacityHigh HighPerformance
    disabled
RC


# vendor.modem_svc: never started. sol has no modem, and starting
# 6080000.remoteproc-mss resets the SoC (see sol-unbind-modem-remoteproc.service).
# It is class core, started at `on boot`. The reset follows the trigger by
# ~35-40 s, so a retest must watch well past 60 s after `on boot`.
# Class changed as well as `disabled`, as for surfaceflinger.
stub_sm_rc "$ROOTFS/vendor/etc/init/vendor.google.modem_svc.rc" <<'RC'
service vendor.modem_svc /vendor/bin/modem_svc -q
    class sol_never_started
    user root
    group system
    capabilities NET_BIND_SERVICE
    disabled
RC

# displayoffload: started; it draws the always-on watchface. The rc is replaced
# because the stock one marks it `disabled` and starts it from an
# `on property:vendor.wac.state=booting` block. It needs the display mux node,
# chowned further down; without that it exits with EACCES every 5 s.
stub_sm_rc "$ROOTFS/vendor/etc/init/vendor.google_clockwork.displayoffload@2.1-service.1p.rc" <<'RC'
service vendor.displayoffload-hal-2-1 /vendor/bin/hw/vendor.google_clockwork.displayoffload@2.1-service.1p
    class late_start
    user system
    group system
RC

# Park three vendor services that crash-loop here: time_daemon (no CAP_SYS_TIME
# in the container's bounding set), vendor.per_mgr (exits 255) and
# vendor.per_proxy (started off per_mgr). Together 36 starts a minute, which
# keeps the watch from staying asleep. They share rc files with other services,
# so only their class is rewritten.
for _drc in "$ROOTFS"/vendor/etc/init/*.rc "$ROOTFS"/vendor/etc/init/hw/*.rc; do
    [ -f "$_drc" ] || continue
    grep -qE "^service (time_daemon|vendor\.per_mgr|vendor\.per_proxy) " "$_drc" || continue
    mountpoint -q "$_drc" && continue
    _dtmp="/run/sol-dead-$(basename "$_drc")"
    awk '
        BEGIN { split("time_daemon vendor.per_mgr vendor.per_proxy", t, " ")
                for (i in t) dead[t[i]] = 1 }
        /^[[:space:]]*service / { inblk = ($2 in dead) }
        inblk && /^[[:space:]]*class / { sub(/class .*/, "class sol_never_started") }
        { print }
    ' "$_drc" > "$_dtmp"
    mount --bind "$_dtmp" "$_drc" \
        && echo "sol-lxc-android: parked crash-looping services in $(basename "$_drc")"
done

# The vendor `on late-fs` (init.sw5100.rc) opens with
# `wait_for_prop hwservicemanager.ready true`. Drop it so init cannot park in
# late-fs before the composer starts. A vendor rc, so it needs its own bind.
for _vrc in "$ROOTFS"/vendor/etc/init/hw/init.*.rc; do
    [ -f "$_vrc" ] || continue
    grep -q "wait_for_prop hwservicemanager.ready" "$_vrc" || continue
    mountpoint -q "$_vrc" && continue
    _vtmp="/run/sol-$(basename "$_vrc")"
    sed -E 's|^([[:space:]]*wait_for_prop hwservicemanager\.ready.*)$|    # HALIUM-SOL: hwservicemanager is stubbed disabled -- \1|' \
        "$_vrc" > "$_vtmp"
    mount --bind "$_vtmp" "$_vrc" \
        && echo "sol-lxc-android: dropped hwservicemanager wait in $(basename "$_vrc")"
done

# fstab: comment out the userdata entry. The container has no userdata block
# device (/data is tmpfs), and `latemount,wait` makes mount_all --late run
# `vdc cryptfs mountFstab`, which blocks in vold forever, so init never reaches
# `class_start hal`. Sign of it: every early_hal service runs, no hal service
# does. Every fstab.* is scanned because the name is board-specific.
for _fst in "$ROOTFS"/vendor/etc/fstab.*; do
    [ -f "$_fst" ] || continue
    grep -qE '^[[:space:]]*/dev/block/bootdevice/by-name/userdata[[:space:]]' "$_fst" || continue
    mountpoint -q "$_fst" && continue
    sed -E 's|^([[:space:]]*/dev/block/bootdevice/by-name/userdata[[:space:]].*)$|# HALIUM-SOL: no real userdata in a HAL-only container, /data is tmpfs -- \1|' \
        "$_fst" > "/run/sol-$(basename "$_fst")"
    mount --bind "/run/sol-$(basename "$_fst")" "$_fst" \
        && echo "sol-lxc-android: neutered userdata latemount in $(basename "$_fst")"
done

# The displayoffload HAL (uid 1000) switches the panel between AP and MCU via
# this node, which the kernel creates root:root 0660. Harmless if absent.
for _mux in /sys/devices/virtual/display/display_comms/display_select \
            /sys/devices/virtual/nanohub/nanohub_comms/display_select; do
    [ -e "$_mux" ] && chown 1000:1000 "$_mux" 2>/dev/null && chmod 0660 "$_mux" 2>/dev/null
done

# /data: vold and init_user0 need something writable. tmpfs is fine for a
# HAL-only container and avoids needing mkfs or a real partition.
if [ -d "$ROOTFS/data" ] && ! mountpoint -q "$ROOTFS/data"; then
    mount -t tmpfs -o rw,nosuid,nodev,mode=0771,size=256M android_data "$ROOTFS/data" \
        && chown 1000:1000 "$ROOTFS/data" 2>/dev/null \
        && echo "sol-lxc-android: mounted tmpfs /data" \
        || echo "sol-lxc-android: WARNING /data tmpfs failed -> init_user0 will fail" >&2
fi

# /linkerconfig must be writable: init writes bootstrap stubs there and the real
# ld.config.txt is generated into it. On the image's read-only empty directory
# no linker config exists, and binaries fail with status 127 as if missing.
if ! mountpoint -q "$ROOTFS/linkerconfig"; then
    mount -t tmpfs -o rw,mode=0755,size=8M android_linkerconfig "$ROOTFS/linkerconfig" \
        && echo "sol-lxc-android: mounted tmpfs /linkerconfig"
fi

# Generate the linker config now. linkerconfig is normally driven by apexd,
# which is masked here (with flattened apexes it hangs and its
# reboot_on_failure kills the container). Without a config, vendor HALs fail
# with "cannot execv" although the binaries exist.
if [ -x "$ROOTFS/apex/com.android.runtime/bin/linkerconfig" ]; then
    mount -t proc proc "$ROOTFS/proc" 2>/dev/null
    mount --rbind /dev "$ROOTFS/dev" 2>/dev/null; mount --make-rslave "$ROOTFS/dev" 2>/dev/null
    mount --rbind /sys "$ROOTFS/sys" 2>/dev/null; mount --make-rslave "$ROOTFS/sys" 2>/dev/null
    chroot "$ROOTFS" /apex/com.android.runtime/bin/linkerconfig --target /linkerconfig 2>&1 \
        | grep -viE "^linker: |^WARNING: linker" | head -3
    umount -R "$ROOTFS/sys" 2>/dev/null; umount -R "$ROOTFS/dev" 2>/dev/null; umount "$ROOTFS/proc" 2>/dev/null
    echo "sol-lxc-android: linkerconfig -> $(ls "$ROOTFS/linkerconfig" 2>/dev/null | tr '\n' ' ')"
fi

# /mnt tmpfs plus /persist (SOL: mmcblk0p16, already mounted host-side).
if ! mountpoint -q $ROOTFS/mnt; then
    mount -t tmpfs android_mnt $ROOTFS/mnt
    mkdir -p $ROOTFS/mnt/vendor/persist
fi
if mountpoint -q /persist && ! mountpoint -q $ROOTFS/mnt/vendor/persist; then
    mount --bind /persist $ROOTFS/mnt/vendor/persist \
        && echo "sol-lxc-android: bound /persist"
fi

# /apex: our A14 image already has the flattened apexes in /apex; a tmpfs over
# it would hide the linker and the first exec would fail. aurora's Halium A13
# image keeps them under /system/apex, so bind them in.
if [ -x "$ROOTFS/apex/com.android.runtime/bin/linker" ]; then
    echo "sol-lxc-android: /apex already populated in the image, leaving it alone"
else
    mountpoint -q $ROOTFS/apex || mount -t tmpfs -o mode=755 android_apex $ROOTFS/apex
    for apex in com.android.runtime com.android.art com.android.i18n; do
        dst=$ROOTFS/apex/$apex
        mountpoint -q "$dst" && continue
        for suffix in .release .debug ""; do
            src=$ROOTFS/system/apex/$apex$suffix
            [ -d "$src" ] || continue
            mkdir -p "$dst"
            mount --bind "$src" "$dst"
            mount -o remount,ro,bind "$dst"
            echo "sol-lxc-android: bound $apex"
            break
        done
    done
fi
# SELinux: mount selinuxfs and load a policy. `/init second_stage` skips
# first-stage init, which normally does both. Without selinuxfs init cannot set
# any property and stalls at 2 processes; with selinuxfs but no policy it blocks
# at wait_for_coldboot_done. androidboot.selinux=permissive keeps denials
# logged, not enforced.
setup_selinux() {
    if ! mountpoint -q /sys/fs/selinux 2>/dev/null; then
        mkdir -p /sys/fs/selinux 2>/dev/null
        mount -t selinuxfs selinuxfs /sys/fs/selinux 2>/dev/null \
            && echo "sol-lxc-android: mounted selinuxfs" \
            || { echo "sol-lxc-android: WARNING selinuxfs mount failed" >&2; return 0; }
    fi

    # Already loaded (e.g. restarting the container)? Leave it alone: reloading
    # is unnecessary and re-labels the running host for no gain.
    if [ -s /sys/fs/selinux/policy ] 2>/dev/null; then
        echo "sol-lxc-android: SELinux policy already loaded ($(wc -c < /sys/fs/selinux/policy) bytes)"
        return 0
    fi

    # Compile exactly the pieces selinux_setup would. The mapping file must match
    # the vendor's expected platform version, which vendor states itself.
    vers=$(cat "$ROOTFS/vendor/etc/selinux/plat_sepolicy_vers.txt" 2>/dev/null | tr -d '[:space:]')
    [ -n "$vers" ] || { echo "sol-lxc-android: WARNING no plat_sepolicy_vers.txt, skipping policy" >&2; return 0; }

    set -- "$ROOTFS/system/etc/selinux/plat_sepolicy.cil" \
           "$ROOTFS/system/etc/selinux/mapping/$vers.cil" \
           "$ROOTFS/system_ext/etc/selinux/system_ext_sepolicy.cil" \
           "$ROOTFS/system_ext/etc/selinux/mapping/$vers.cil" \
           "$ROOTFS/product/etc/selinux/product_sepolicy.cil" \
           "$ROOTFS/product/etc/selinux/mapping/$vers.cil" \
           "$ROOTFS/vendor/etc/selinux/plat_pub_versioned.cil" \
           "$ROOTFS/vendor/etc/selinux/vendor_sepolicy.cil"
    for f in "$@"; do
        [ -f "$f" ] || { echo "sol-lxc-android: WARNING missing policy piece $f, skipping" >&2; return 0; }
    done

    out=/run/sol-sepolicy.bin
    if ! "$ROOTFS/system/bin/secilc" -m -M true -G -N -c 30 "$@" -o "$out" -f /dev/null 2>/dev/null; then
        echo "sol-lxc-android: WARNING secilc failed, container will stall in init" >&2
        return 0
    fi

    # Must be a single write(): selinuxfs 'load' takes the policy in one go, and
    # cat's 128K chunks fail silently.
    sz=$(wc -c < "$out")
    if dd if="$out" of=/sys/fs/selinux/load bs="$sz" count=1 2>/dev/null; then
        echo "sol-lxc-android: SELinux policy loaded ($sz bytes, enforce=$(cat /sys/fs/selinux/enforce 2>/dev/null))"
    else
        echo "sol-lxc-android: WARNING policy load failed" >&2
    fi
    rm -f "$out"
}
setup_selinux

# HIDL sensors bridge (see the sol-sensors-hidl recipe). After setup_selinux,
# because it mounts a tmpfs with -o context=, which fails without a policy.
# Before lxc-start, because init execs the sensors service from the bound path
# and hwservicemanager reads VINTF once. Non-fatal: no sensors beats no UI.
if [ -x /usr/libexec/sol-sensors-hidl.sh ]; then
    /usr/libexec/sol-sensors-hidl.sh || \
        echo "sol-lxc-android: WARNING sol-sensors-hidl.sh failed, sensors will be absent" >&2
fi

# Patch init.rc to restore /apex. mount-binder.sh (lxc.hook.mount) stashes the
# flattened /apex at /postinstall before init starts, and init mounts a tmpfs
# over /apex several times. The rebind must use init's own `mount` builtin:
# every mount binary in the container needs /apex/.../linker, which is what is
# missing. The base is the mounted image's init.rc, so its existing edits stay.
APEX_STASH=/postinstall
if [ -d "$ROOTFS$APEX_STASH" ] && [ -f "$ROOTFS/system/etc/init/hw/init.rc" ]; then
    if ! mountpoint -q "$ROOTFS/system/etc/init/hw/init.rc"; then
        _irc=/run/sol-init.rc
        awk -v stash="$APEX_STASH" '
            # Rules that rewrite $0 must come before the print rule below.
            #
            # Keep init from mounting its own binderfs. mount-binder.sh binds the host
            # binderfs in, and binder contexts do not span instances, so a second mount
            # would cut libhybris off from the container composer.
            /^[[:space:]]*mount binder binder \/dev\/binderfs/ {
                $0 = "    # HALIUM-SOL: host binderfs bind-mounted instead -- " $0
            }
            # installkey enables FBE on /data, which fails on tmpfs, and init then
            # reboots the container (enablefilecrypto_failed).
            /^[[:space:]]*installkey \/data[[:space:]]*$/ {
                $0 = "    # HALIUM-SOL: no FBE on a tmpfs /data -- " $0
            }
            # Without an FBE key every encryption=Require mkdir fails and init reboots;
            # DeleteIfNecessary even deletes the directory. Downgrade them to None so the
            # directories are still created with the right owner and mode.
            /^[[:space:]]*mkdir .*encryption=(Require|Attempt|DeleteIfNecessary)/ {
                sub(/encryption=(Require|Attempt|DeleteIfNecessary)/, "encryption=None")
            }
            # The rules below drop waits and blocking execs that never return in a
            # HAL-only container (tmpfs /data, flattened apexes, no apexd, no A/B
            # update). Each one parks init before class_start hal.
            #
            # update_verifier: blocks in pipe_read; there is no update to verify.
            /^[[:space:]]*exec_start update_verifier/ {
                $0 = "    # HALIUM-SOL: no A/B update to verify here -- " $0
            }
            # apexd execs such as apexd-snapshotde: apexd never runs here.
            /^[[:space:]]*exec_start apexd/ {
                $0 = "    # HALIUM-SOL: apexes are flattened, apexd never runs -- " $0
            }
            # start zygote: no framework (see the zygote stub). An explicit start
            # overrides `disabled` in init.zygote32.rc.
            /^[[:space:]]*start zygote(_secondary)?[[:space:]]*$/ {
                $0 = "    # HALIUM-SOL: no framework in a HAL-only container -- " $0
            }
            # init_user0 is a builtin, but it calls vold and hangs the same way.
            /^[[:space:]]*init_user0[[:space:]]*$/ {
                $0 = "    # HALIUM-SOL: no real user0 on a tmpfs /data -- " $0
            }
            # Every exec of vdc is a waiting call into vold (checkpoint markBootAttempt,
            # keymaster earlyBootEnded, ...) and hangs.
            /^[[:space:]]*exec .*\/system\/bin\/vdc / {
                $0 = "    # HALIUM-SOL: vold lifecycle is meaningless on tmpfs /data -- " $0
            }
            # apexd.status is never set because apexd never runs. Dropping the wait
            # beats setting it early: something rewrites it after a manual setprop.
            /^[[:space:]]*wait_for_prop apexd\.status activated[[:space:]]*$/ {
                $0 = "    # HALIUM-SOL: apexes are flattened, apexd never runs -- " $0
            }
            { print }
            # Rebind the stash over /apex at the start of each early stage.
            /^on (early-init|init|late-init|post-fs|post-fs-data)$/ {
                print "    # HALIUM-SOL: restore flattened /apex hidden by init'"'"'s tmpfs"
                print "    mount none " stash " /apex bind rec"
            }
            # Disable UBWC in QTI gralloc before any display service starts. monaco
            # plane 70, the only plane the launcher can use, cannot scan UBWC, so the
            # atomic commit fails with EINVAL ("plane70 ubwc not supported") and the
            # panel stays dark. The allocator and the composer read the property only at
            # startup. libhybris also needs libhybris-sol-disable-ubwc-usage.patch:
            # HWComposerNativeWindow requests GRALLOC_USAGE_HW_COMPOSER, which turns
            # UBWC on regardless.
            /^on init$/ {
                print "    # HALIUM-SOL: plane 70 cannot scan UBWC -- see sol-lxc-android-start.sh"
                print "    setprop vendor.gralloc.disable_ubwc 1"
                # The health HAL is masked (see above). The battery listener of the QTI
                # audio HAL waits for android.hardware.health.IHealth/default forever,
                # inside module-droid-card of PulseAudio, so PulseAudio never starts.
                # vendor/build.prop enables the listener; this runs after it is read.
                print "    # HALIUM-SOL: health HAL masked, audio HAL must not wait for it"
                print "    setprop vendor.audio.feature.battery_listener.enable false"
            }
            # Start halium-apexfix from the existing post-fs-data block; an appended
            # `on post-fs-data` section was seen not to fire.
            /^on post-fs-data$/ {
                print "    write /dev/kmsg HALIUM-SOL-MARKER-apexfix"
                print "    start halium-apexfix"
            }
            # Generate the linker config at early-init. Stock runs the full
            # perform_apex_config only in post-fs-data and relies on first-stage apexd.
            # Without a config keystore2 cannot link (libandroidicu.so lives in
            # com.android.i18n), vold retries forever in late-fs, and init never reaches
            # post-fs-data to generate it: a deadlock.
            /^on early-init$/ {
                print "    exec - root root -- /apex/com.android.runtime/bin/linkerconfig --target /linkerconfig"
            }
            # perform_apex_config, both forms, mounts a fresh tmpfs over /apex: rebind
            # and regenerate the linker config right after each.
            /^[[:space:]]*perform_apex_config --bootstrap[[:space:]]*$/ {
                print "    # HALIUM-SOL: restore flattened /apex hidden by init'"'"'s tmpfs"
                print "    mount none " stash " /apex bind rec"
                print "    exec - root root -- /apex/com.android.runtime/bin/linkerconfig --target /linkerconfig"
            }
            /^[[:space:]]*perform_apex_config[[:space:]]*$/ {
                print "    # HALIUM-SOL: restore flattened /apex hidden by init'"'"'s tmpfs"
                print "    mount none " stash " /apex bind rec"
                print "    exec - root root -- /apex/com.android.runtime/bin/linkerconfig --target /linkerconfig"
            }
            # init switches mount namespace mid-boot, discarding the `on <stage>`
            # rebinds made in the bootstrap namespace. A service is forked into the
            # current namespace, so its bind survives. toybox runs through
            # /system/bin/bootstrap/linker because /apex is empty at that point.
            # No "restore flattened" marker here: the guard below expects exactly 7.
            END {
                print ""
                print "service halium-apexfix /system/bin/bootstrap/linker /system/bin/toybox mount -o bind " stash " /apex"
                print "    class core"
                print "    user root"
                print "    group root"
                print "    oneshot"
                # init rejects the bootstrap linker label for a service exec. That is an
                # init-side check, so permissive does not help; seclabel bypasses it.
                print "    seclabel u:r:su:s0"
                print ""
                print "on post-fs-data"
                print "    write /dev/kmsg HALIUM-SOL-MARKER-apexfix"
                print "    start halium-apexfix"
            }' "$ROOTFS/system/etc/init/hw/init.rc" > "$_irc"
        if [ "$(grep -c 'HALIUM-SOL: restore flattened' "$_irc")" = 7 ]; then
            mount --bind "$_irc" "$ROOTFS/system/etc/init/hw/init.rc" \
                && echo "sol-lxc-android: init.rc patched to rebind /apex from $APEX_STASH"
        else
            echo "sol-lxc-android: WARNING init.rc apex rebind injection failed, not binding" >&2
        fi
    fi
fi

echo "sol-lxc-android: /apex now: $(ls $ROOTFS/apex 2>/dev/null | tr '\n' ' ')"
echo "sol-lxc-android: linker present: $([ -x $ROOTFS/apex/com.android.runtime/bin/linker ] && echo yes || echo NO)"

echo "sol-lxc-android: /config/usb_gadget before container start:"
for g in /config/usb_gadget/*; do
    [ -d "$g" ] || continue
    echo "  $g UDC=$(cat "$g/UDC" 2>/dev/null)"
done

api_level=$(sed -n 's/^ro.build.version.sdk=//p' $ROOTFS/system/build.prop)
echo "sol-lxc-android: starting container 'android' (api ${api_level:-?})..."

# Launch second-stage init directly: the host did first-stage's job, and a bare
# /init runs FirstStageMain, whose re-exec SIGSEGVs.

exec /usr/bin/lxc-start -n android -F -P /var/lib/lxc -- \
    /system/bin/env -i \
        PATH=/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/sbin:/system/sbin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin \
        INIT_STARTED_AT=0 \
        /init second_stage
