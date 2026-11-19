#!/bin/sh
# Recreate the dm-linear mappings for sol's stock Android-14 logical partitions
# inside /super (/dev/mmcblk0p80, slot _a) and mount them under /android/ so the
# Halium LXC container can bind them in.
#
# Offsets from `lpdump` on stock AW2A.240903, slot A. Re-derive if Google
# reflashes super.
#   system_a       : single segment, offset 2048,    len 3679272 (~1.75 GiB)
#   vendor_a       : single segment, offset 3681320, len 521392  (~254 MiB)
#   vendor_dlkm_a  : single segment, offset 6792968, len 120512  (~59 MiB)
#   system_dlkm_a  : single segment, offset 6913480, len 21616   (~10.5 MiB)
set -e
SUPER="/dev/mmcblk0p80"
if [ ! -b "$SUPER" ]; then
    echo "sol-vendor-mount: $SUPER not present, aborting" >&2
    exit 1
fi
dm_create_if_missing() {
    name=$1; shift; table=$1
    if [ -b /dev/mapper/$name ]; then
        echo "sol-vendor-mount: /dev/mapper/$name exists, skipping"
    else
        printf "%s" "$table" | dmsetup create $name
        echo "sol-vendor-mount: created /dev/mapper/$name"
    fi
}
# system_ext and product carry sepolicy fragments the vendor policy references.
# Android init compiles the CIL at boot (sol ships no precompiled_sepolicy), and
# without system_ext secilc fails on the first vendor rule using a type it
# declares:
#     /system/bin/secilc: Invalid genfscon at /vendor/etc/selinux/vendor_sepolicy.cil:76
# (sysfs_rootdisk_tuning, declared only in
# /system_ext/etc/selinux/system_ext_sepolicy.cil).
dm_create_if_missing product_a      "0 2365104 linear $SUPER 4202712"
dm_create_if_missing system_ext_a   "0 222032 linear $SUPER 6567816"
dm_create_if_missing vendor_a       "0 521392 linear $SUPER 3681320"
dm_create_if_missing vendor_dlkm_a  "0 120512 linear $SUPER 6792968"
dm_create_if_missing system_a       "0 3679272 linear $SUPER 2048"
dm_create_if_missing system_dlkm_a  "0 21616 linear $SUPER 6913480"
# odm_a: vold enumerates super's logical partitions and aborts when one is
# absent:
#     Abort message: 'could not find logical partition odm_a: No such device'
# vold then crash-loops, `late-fs` fails, and boot stalls before the vendor rc
# stage that starts the display composer.
dm_create_if_missing odm_a          "0 3120 linear $SUPER 6789848"

mount_if_unmounted() {
    src=$1; dst=$2; mkdir -p $dst
    if mountpoint -q $dst; then echo "sol-vendor-mount: $dst already mounted"
    else mount -o ro,nosuid,nodev $src $dst; echo "sol-vendor-mount: $dst <- $src"; fi
}
mount_if_unmounted /dev/mapper/system_a      /android/system
mount_if_unmounted /dev/mapper/vendor_a      /android/vendor
mount_if_unmounted /dev/mapper/vendor_dlkm_a /android/vendor_dlkm
mount_if_unmounted /dev/mapper/product_a    /android/product
mount_if_unmounted /dev/mapper/system_ext_a /android/system_ext
# odm is mounted, not just mapped, to break a symlink loop: the container image
# has /odm/etc -> /vendor/odm/etc and sol's vendor has /vendor/odm -> /odm.
# libvintf's lookup of the SKU manifest (ro.boot.hardware.sku=GGE4J) then hits
# ELOOP, which fails the whole device-manifest fetch:
#     Cannot read /odm/etc/vintf/manifest_GGE4J.xml: Too many symbolic links encountered
# servicemanager is left with no vendor manifest and rejects every vendor HAL
# (composer, keymint, boot, power, ...). With the real odm_a mounted the lookup
# returns ENOENT, which libvintf tolerates for an optional SKU manifest.
mount_if_unmounted /dev/mapper/odm_a        /android/odm

# libhybris's android linker searches
# /usr/libexec/hal-droid/system/lib:/vendor/lib:/system/lib (set in the
# libhybris bbappend), so /vendor here and /system below are how the stock
# Android libraries become reachable.
[ -L /vendor ] || { rm -rf /vendor 2>/dev/null; ln -sf /android/vendor /vendor; }

# /system is the host's Android system tree, used by libhybris in
# asteroid-launcher, sensorfwd, ngfd and bluebinder. It is separate from the
# container rootfs because the two need different releases:
#   host      -> Halium A13. Stock A14 bionic dereferences NULL (+0x410) before
#                main(), because libhybris skips libc.so's constructors.
#   container -> A14. A13 init cannot parse sol's A14
#                vendor_property_contexts and aborts in PropertyInit.
# Test for the image, not the mountpoint: var-lib-lxc-android-rootfs.mount is
# ordered After= this service, and a symlink only needs its target when it is
# followed. The stock fallback is system-as-root, hence /android/system/system.
if [ -f /var/lib/lxc/android/hybris-system.img ]; then
    SYSTEM_TARGET=/android/hybris/system
elif [ -d /android/system/system ]; then
    SYSTEM_TARGET=/android/system/system
else
    SYSTEM_TARGET=/android/system
fi
# Re-point unconditionally: `[ -L /system ]` is true for a dangling link too,
# so that guard could never repair a broken link from a previous boot.
if [ ! -L /system ] || [ "$(readlink /system)" != "$SYSTEM_TARGET" ]; then
    rm -rf /system 2>/dev/null
    ln -sfn "$SYSTEM_TARGET" /system
fi
echo "sol-vendor-mount: /system -> $SYSTEM_TARGET"

# --- APEX: mount the runtime apexes so /system/lib's bionic symlinks resolve ---
#
# On Android 10+ bionic lives in an APEX:
#   /system/lib/libc.so  -> /apex/com.android.runtime/lib/bionic/libc.so
#   /system/lib/libm.so  -> /apex/com.android.runtime/lib/bionic/libm.so
#   /system/lib/libdl.so -> /apex/com.android.runtime/lib/bionic/libdl.so
# apexd mounts these under Android init, which the host does not run.
#
# An .apex is a zip whose first entry, apex_payload.img, is an ext4 image stored
# uncompressed and page-aligned, so it can be loop-mounted in place at its
# offset: no unzip, no RAM cost. The offset is parsed from the zip local header
# because the alignment padding is not guaranteed.
mount_apex() {
    _file="/system/apex/$1.apex"; _name="$2"
    [ -f "$_file" ] || { echo "sol-vendor-mount: apex $1 absent, skipping"; return 0; }
    mountpoint -q "/apex/$_name" && return 0
    # local file header: 26=filename len, 28=extra len, data starts at 30+n+e
    _n=$(od -An -tu2 -j26 -N2 "$_file" 2>/dev/null | tr -d ' ')
    _e=$(od -An -tu2 -j28 -N2 "$_file" 2>/dev/null | tr -d ' ')
    case "$_n$_e" in ''|*[!0-9]*) echo "sol-vendor-mount: apex $1 header unreadable, skipping" >&2; return 0 ;; esac
    # A compressed apex holds a deflated `original_apex` instead, which cannot
    # be mounted in place (com.google.android.art is one). Check the entry name,
    # or the arithmetic above yields a nonsense offset.
    _nm=$(dd if="$_file" bs=1 skip=30 count="$_n" 2>/dev/null)
    if [ "$_nm" != "apex_payload.img" ]; then
        echo "sol-vendor-mount: apex $1 is not mountable in place (first entry '$_nm'), skipping"
        return 0
    fi
    _off=$((30 + _n + _e))
    mkdir -p "/apex/$_name"
    if mount -o ro,loop,offset=$_off "$_file" "/apex/$_name" 2>/dev/null; then
        echo "sol-vendor-mount: /apex/$_name <- $1.apex (offset $_off)"
        return 0
    fi
    # Fallback for a mount(8) without offset= support: explicit loop device.
    _ld=$(losetup -f 2>/dev/null)
    if [ -n "$_ld" ] && losetup -r -o $_off "$_ld" "$_file" 2>/dev/null \
            && mount -o ro "$_ld" "/apex/$_name" 2>/dev/null; then
        echo "sol-vendor-mount: /apex/$_name <- $1.apex via $_ld (offset $_off)"
    else
        echo "sol-vendor-mount: WARNING could not mount apex $1 (offset $_off)" >&2
    fi
}
# Skipped when the container rootfs image is present: sol-android-apex.service
# then binds Halium's flattened A13 apexes, and an A13 system must not be
# paired with A14 bionic.
if [ ! -f /var/lib/lxc/android/android-rootfs.img ] && [ -d /system/apex ]; then
    # runtime carries bionic; i18n carries ICU. com.google.android.art is not
    # mounted: it is compressed, and nothing in /system/lib links into it.
    mount_apex com.android.runtime     com.android.runtime
    mount_apex com.android.i18n        com.android.i18n
fi
if [ -e /system/lib/libc.so ]; then
    echo "sol-vendor-mount: bionic OK (/system/lib/libc.so resolves)"
else
    echo "sol-vendor-mount: WARNING /system/lib/libc.so does not resolve -- the launcher will fail" >&2
fi

# Library search order is fixed in the libhybris bbappend (/system/lib ahead of
# /vendor/lib). If "cannot locate symbol ... openDeclaredPassthroughHal" comes
# back, check that path before adding a shim here. The directory is kept for
# libui_compat_layer.so.
mkdir -p /usr/libexec/hal-droid/system/lib

# cs40l26 haptics: request_firmware from /vendor/firmware fails at initramfs time
# (no /vendor yet); re-bind now that /vendor exists.
if [ -d /sys/bus/i2c/drivers/cs40l26 ] && [ -d /sys/bus/i2c/devices/0-0043 ] \
        && [ ! -L /sys/bus/i2c/devices/0-0043/driver ]; then
    echo 0-0043 > /sys/bus/i2c/drivers/cs40l26/bind 2>/dev/null \
        && echo "sol-vendor-mount: bound cs40l26 to 0-0043"
fi

# Modem firmware lives on the modem partition (sol = /dev/mmcblk0p43), VFAT,
# under image/. Needed for WLAN (icnss/WLFW) and BT. Mount it host-side and
# point firmware_class.path at it.
if [ -b /dev/mmcblk0p43 ] && ! mountpoint -q /vendor/firmware_mnt 2>/dev/null; then
    mkdir -p /vendor/firmware_mnt 2>/dev/null || true
    mount -t vfat -o ro,uid=1000,gid=1000,fmask=0337,dmask=0227 \
            /dev/mmcblk0p43 /vendor/firmware_mnt 2>/dev/null \
        && echo "sol-vendor-mount: mounted firmware_mnt (mmcblk0p43)" || true
fi
# --- Adreno GPU firmware reachability ---
#
# Opening /dev/kgsl-3d0 makes KGSL request_firmware(a702_sqe.fw), which ships in
# /vendor/firmware but the kernel only searches /lib/firmware and
# firmware_class.path. Without it the open returns ENOENT:
#     kgsl-3d 5900000.qcom,kgsl-3d0: Direct firmware load for a702_sqe.fw failed with error -2
# Symlinks, because firmware_class.path holds one directory and the modem
# image path below needs it.
if [ -d /vendor/firmware ]; then
    mkdir -p /lib/firmware
    for _f in /vendor/firmware/a[0-9][0-9][0-9]_* /vendor/firmware/gen[0-9]*; do
        [ -e "$_f" ] || continue
        ln -sf "$_f" "/lib/firmware/$(basename "$_f")"
    done
    echo "sol-vendor-mount: linked Adreno firmware into /lib/firmware"
fi

if [ -f /vendor/firmware_mnt/image/modem.mdt ]; then
    echo /vendor/firmware_mnt/image > /sys/module/firmware_class/parameters/path
    echo "sol-vendor-mount: firmware_class.path -> /vendor/firmware_mnt/image"
fi
# Do not start the modem remoteproc (remoteproc1, 6080000.remoteproc-mss).
# This is the BT/WiFi variant with no modem (kernel log: modem_supported=0x0).
# A failed MSS bring-up escalates to a full SoC reset ~35-40 s after the kick.
# remoteproc1 exists and reports "offline" here too, so an LTE sol port must
# gate the kick on real modem presence, not on the node. The firmware mount
# above stays: WLAN and BT firmware are read from it.
