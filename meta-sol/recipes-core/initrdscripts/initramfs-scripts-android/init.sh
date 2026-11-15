#! /bin/sh

. /machine.conf

# Log to /dev/kmsg: CONFIG_TTY_PRINTK=n (GKI default), so /dev/ttyprintk
# never exists. printk.devkmsg=on is set on our cmdline.
info() { echo "init: $1" > /dev/kmsg 2>/dev/null; }
# Hold, never reboot. On an A/B device a reboot loop burns one of the 7 slot
# retries per cycle until the bootloader falls back to stock, and every reset
# overwrites the RAM console that logdump would have carried the reason in.
# adbd is already running at the only caller (the sdcard mount), so this leaves
# a root shell at the point of failure:
#   adb -s 0123456789ABCDEF shell
fail() {
    echo "init: Failed" > /dev/kmsg 2>/dev/null
    echo "init: $1" > /dev/kmsg 2>/dev/null
    # Dump the state that explains a rootfs-stage failure, while we still have it.
    { echo "init: FAILDIAG sdcard_partition=$sdcard_partition"
      echo "init: FAILDIAG mmcblk_nodes=$(ls -d /dev/mmcblk* 2>/dev/null | wc -l)"
      echo "init: FAILDIAG p81_present=$([ -e /dev/mmcblk0p81 ] && echo yes || echo no)"
      echo "init: FAILDIAG mounts=[$(cat /proc/mounts 2>/dev/null | awk '{print $1":"$3}' | tr '\n' ' ')]"
      echo "init: FAILDIAG BOOT_DIR=$BOOT_DIR"
    } > /dev/kmsg 2>/dev/null
    echo "init: HOLDING with adb alive -- NOT rebooting. Shell in and look around." > /dev/kmsg 2>/dev/null
    while true; do
        sleep 60
        echo "init: still holding after failure: $1" > /dev/kmsg 2>/dev/null
    done
}

setup_devtmpfs() {
    mount -t devtmpfs -o mode=0755,nr_inodes=0 devtmpfs $1/dev
    mkdir $1/dev/pts
    mount -t devpts none $1/dev/pts/
    test -c $1/dev/fd     || ln -sf /proc/self/fd $1/dev/fd
    test -c $1/dev/stdin  || ln -sf fd/0 $1/dev/stdin
    test -c $1/dev/stdout || ln -sf fd/1 $1/dev/stdout
    test -c $1/dev/stderr || ln -sf fd/2 $1/dev/stderr
    test -c $1/dev/socket || mkdir -m 0755 $1/dev/socket
}

info "Mounting relevant filesystems ..."
mkdir -m 0755 /proc;  mount -t proc proc /proc
mkdir -m 0755 /sys;   mount -t sysfs sys /sys
mkdir -p /dev;        setup_devtmpfs ""

# The cmdline sets loglevel=6, which drops every KERN_INFO line from the RAM
# console that ends up in logdump, so a driver that fails once and recovers on
# deferred probe looks like one that never recovers. 7 admits KERN_INFO but not
# KERN_DEBUG, which could wrap the fixed-size RAM console. This runs at ~0.6s,
# before clock/regulator/interconnect bring-up at ~1.4s.
echo 7 > /proc/sys/kernel/printk 2>/dev/null

# ---- Load the vendor module chain ----
# vendor_kernel_boot ships the .ko files flat in /lib/modules with absolute
# paths in modules.dep. busybox modprobe chdir()s into /lib/modules/$(uname -r),
# so point that at the flat directory. modprobe then resolves dependencies, and
# the order in modules.load.sol does not matter.
KREL=$(uname -r)
[ ! -e "/lib/modules/$KREL" ] && ln -sf . "/lib/modules/$KREL"

# sol's USB gate is google_extcon_shim (aurora's google-extcon-usb-shim does not
# exist here). Without it opened dwc3-msm registers no UDC and there is no adb.
# The gate is a pair of device sysfs attrs, opened further down.
info "Pre-loading google-extcon-shim ..."
modprobe google-extcon-shim 2>/dev/kmsg

info "Loading sol kernel modules from /etc/modules.load.sol ..."
while read mod; do
    case "$mod" in ''|\#*) continue ;; esac
    modprobe "${mod%.ko}" 2>/dev/kmsg
done < /etc/modules.load.sol
info "Loaded sol kernel modules: $(ls /sys/bus/platform/drivers/ 2>/dev/null | wc -l) platform drivers registered"

# --- diagnostics, to /dev/kmsg ---
# logdump (mmcblk0p2) holds the persistent RAM console, which the bootloader
# writes out across a reset, so this also captures boots where storage never
# comes up. Read it back from rooted stock with:
#   dd if=/dev/block/by-name/logdump bs=1M count=8 | strings
# Bound devices are reported, not registered drivers: a bound device is a
# symlink inside the driver's sysfs dir.
soldiag() {
    info "DIAG $1: uptime=$(cut -d. -f1 /proc/uptime)s dev=$(ls /dev 2>/dev/null | wc -l) modules=$(wc -l < /proc/modules)"
    # Count, don't list: /dev/kmsg truncates a write at ~1KB and the eMMC has
    # ~90 partitions.
    info "DIAG $1: mmcblk_nodes=$(ls -d /dev/mmcblk* 2>/dev/null | wc -l) rootdisk=$([ -e /dev/mmcblk0 ] && echo yes || echo no) udc=[$(ls /sys/class/udc 2>/dev/null | tr '\n' ' ')]"
    # The USB chain: which extcons exist, and whether the dwc3-msm platform
    # device exists at all (as opposed to present but unbound).
    for e in /sys/class/extcon/*; do
        [ -e "$e/name" ] && info "DIAG $1: extcon $(basename "$e") name=$(cat "$e/name" 2>/dev/null)"
    done
    info "DIAG $1: hsusb_dev=[$(ls -d /sys/bus/platform/devices/*hsusb* /sys/bus/platform/devices/*dwc3* 2>/dev/null | sed 's|.*/||' | tr '\n' ' ')]"
    # SPMI/PMIC layer. qpnp-smblite, the USB extcon supplier, is a pm5100 child
    # created by devm_of_platform_populate() at the end of pmic_spmi_probe()
    # (drivers/mfd/qcom-spmi-pmic.c). These lines tell apart: spmi device 0-00
    # missing, present but not probed, or probe failed before populate.
    info "DIAG $1: spmi_drivers=[$(ls /sys/bus/spmi/drivers 2>/dev/null | tr '\n' ' ')]"
    for d in /sys/bus/spmi/devices/*; do
        [ -e "$d" ] || continue
        info "DIAG $1: spmi dev $(basename "$d") driver=$(basename "$(readlink -f "$d/driver" 2>/dev/null)" 2>/dev/null)"
    done
    info "DIAG $1: pm5100_children=$(ls /sys/bus/platform/devices 2>/dev/null | grep -c pm5100)"
    # devices_deferred lists each deferred device with its reason string
    # (drivers/base/dd.c:deferred_devs_show). One line per write, for the 1KB limit.
    mount -t debugfs debugfs /sys/kernel/debug 2>/dev/null
    if [ -r /sys/kernel/debug/devices_deferred ]; then
        n=0
        while read -r dl; do
            n=$((n+1))
            info "DIAG $1: deferred[$n] $dl"
        done < /sys/kernel/debug/devices_deferred
        info "DIAG $1: deferred_total=$n"
    else
        info "DIAG $1: devices_deferred UNAVAILABLE"
    fi
    for a in force_disable spoof_connected; do
        for f in /sys/bus/platform/devices/*extcon*/$a; do
            [ -e "$f" ] && info "DIAG $1: shim $a=$(cat "$f" 2>/dev/null)"
        done
    done
    for drv in rpm-smd qcom-smd-rpm clk-smd-rpm qcom_glink_rpm rpm-smd-regulator \
               qnoc-monaco gcc-monaco sdhci_msm msm-dwc3 dwc3 gdsc-regulator; do
        d=/sys/bus/platform/drivers/$drv
        [ -d "$d" ] || { info "DIAG $1: drv $drv ABSENT"; continue; }
        # Everything except the four standard attrs is a bound device. Plain
        # ls + grep, because busybox find is not guaranteed here.
        info "DIAG $1: drv $drv bound=[$(ls "$d" 2>/dev/null | grep -vE '^(bind|unbind|uevent|module)$' | tr '\n' ' ')]"
    done
}
soldiag after-modules

# Replay the start of the ring buffer, which the console filtered before the
# loglevel bump. Snapshot to a file first: piping dmesg into /dev/kmsg would
# append to the buffer being read.
dmesg 2>/dev/null | head -200 > /predmesg.txt
while read -r l; do echo "PRE: $l" > /dev/kmsg 2>/dev/null; done < /predmesg.txt

# Open the USB gate: force_disable=0 un-gates dwc3-msm, and spoof_connected=1
# because the charging cradle gives no real USB connect event for the extcon.
for base in /sys/bus/platform/devices/*extcon*/ /sys/devices/platform/soc/*extcon*/ ; do
    [ -e "${base}force_disable" ]   && echo 0 > "${base}force_disable"   2>/dev/kmsg && info "USB gate: force_disable=0 at $base"
    [ -e "${base}spoof_connected" ] && echo 1 > "${base}spoof_connected" 2>/dev/kmsg && info "USB gate: spoof_connected=1 at $base"
done

# Cache cmdline contents while /proc is still mounted. The proc/sys move
# (umount -l /proc, mount -t proc proc $BOOT_DIR/proc) happens before
# switch_root, so any LATER `grep /proc/cmdline` would fail.
CMDLINE=$(cat /proc/cmdline 2>/dev/null)

# Always start adbd, regardless of `debug-ramdisk`. That flag is not ours: it
# comes from the bootloader and disappears after a factory reset (which clears
# the misc BCB). Without adbd a failed boot has no diagnostic channel at all.
# adbd is backgrounded and boot continues to switch_root.
case "$CMDLINE" in *debug-ramdisk*) DEBUG_RAMDISK=1 ;; *) DEBUG_RAMDISK=0 ;; esac
info "adbd: starting unconditionally (debug-ramdisk on cmdline: $DEBUG_RAMDISK)"
if true; then
    # android-gadget-setup starts with `cd /sys/kernel/config/usb_gadget`, and
    # there is no systemd yet to mount configfs. It then creates functions/ffs.usb0,
    # which registers the functionfs fs type (lazily, on 5.15 GKI), and mounts it
    # at /dev/usb-ffs/adb.
    mkdir -p /sys/kernel/config
    mount -t configfs none /sys/kernel/config 2>/dev/null || true

    /usr/bin/android-gadget-setup adb

    # Legacy /sys/class/android_usb writes. No-ops on 5.15 GKI (no
    # CONFIG_USB_ANDROID), kept for parity with the upstream init.sh.
    echo 0 > /sys/class/android_usb/android0/enable 2>/dev/null
    echo 18d1 > /sys/class/android_usb/android0/idVendor 2>/dev/null
    echo d002 > /sys/class/android_usb/android0/idProduct 2>/dev/null
    echo adb > /sys/class/android_usb/android0/f_ffs/aliases 2>/dev/null
    echo ffs > /sys/class/android_usb/android0/functions 2>/dev/null
    echo AsteroidOS > /sys/class/android_usb/android0/iManufacturer 2>/dev/null
    echo InitRamDisk > /sys/class/android_usb/android0/iProduct 2>/dev/null
    serial="$(sed 's/.*androidboot.serialno=//;s/ .*//' /proc/cmdline)"
    echo "$serial" > /sys/class/android_usb/android0/iSerial 2>/dev/null
    echo 1 > /sys/class/android_usb/android0/enable 2>/dev/null

    /usr/bin/adbd &

    # Bind the gadget to the first UDC. dwc3-msm is a module, so the UDC
    # appears 2-10s into boot.
    info "adbd: usb_gadget=$(cd /sys/kernel/config/usb_gadget 2>/dev/null && echo *)"
    # Poll without command substitution: $(cd /sys/class/udc && echo *) hung
    # indefinitely here.
    UDC=""
    i=0
    while [ $i -lt 60 ]; do
        set -- /sys/class/udc/*
        if [ -e "$1" ]; then UDC=${1##*/}; break; fi
        sleep 1
        i=$((i+1))
    done
    if [ -n "$UDC" ]; then
        UDC=$(echo "$UDC" | awk '{print $1}')
        info "adbd: found UDC=$UDC after $((i+1))s"
        if echo "$UDC" > /sys/kernel/config/usb_gadget/adb/UDC 2>/dev/kmsg; then
            info "adbd: bound UDC=$UDC successfully"
        else
            info "adbd: bind to $UDC FAILED (write error)"
        fi
    else
        info "adbd: NO UDC found after 60s -- dwc3-msm gate still off?"
        set -- /sys/class/udc/*; info "adbd: /sys/class/udc entries = '$1'"
        soldiag no-udc
        # Idle rather than panic. A panic reset left nothing in logdump, while a
        # manual power cycle preserves the RAM console.
        info "DIAG: idling for the operator -- power-cycle now, then read logdump"
        while true; do sleep 3600; done
    fi

    # Fall through to switch_root. The bootloader appends `debug-ramdisk`
    # whenever it boots an unsigned init_boot, so it is on every AsteroidOS boot
    # and cannot mean "stay in the initramfs". As in the meta-asteroid init.sh,
    # it gates only the adbd block. A missing rootfs is caught by the safety net
    # before switch_root.
    info "debug-ramdisk: adbd started; continuing to rootfs (canonical behaviour)"
fi

rotation=0
[ -e /etc/rotation ] && read rotation < /etc/rotation
[ -x /usr/bin/msm-fb-refresher ] && /usr/bin/msm-fb-refresher
/usr/bin/psplash --angle $rotation --no-console-switch &

info "Mounting sdcard..."
mkdir -m 0777 /sdcard /loop
# Bounded on purpose: an unbounded wait hangs the boot with no diagnosis.
# fail() parks with adbd alive instead, which is recoverable.
i=0
while [ ! -e /dev/$sdcard_partition ] && [ $i -lt 30 ]; do
    info "Waiting for $sdcard_partition..."
    sleep 1
    i=$((i+1))
done
[ -e /dev/$sdcard_partition ] || fail "$sdcard_partition never appeared"

/sbin/fsck.ext4 -p /dev/$sdcard_partition
mount -t auto -o rw,noatime,nodiratime /dev/$sdcard_partition /sdcard
[ $? -eq 0 ] || fail "Failed to mount the sdcard. Cannot continue."

info "Checking for loop rootfs image on the sdcard..."
ANDROID_MEDIA_DIR="/sdcard/media/"
[ -d /sdcard/media/0 ]            && ANDROID_MEDIA_DIR="/sdcard/media/0"
[ -e /sdcard/asteroidos.ext4 ]    && ANDROID_MEDIA_DIR="/sdcard/"

BOOT_DIR="/sdcard"
if [ -e $ANDROID_MEDIA_DIR/asteroidos.ext4 ] ; then
    /sbin/fsck.ext4 -p $ANDROID_MEDIA_DIR/asteroidos.ext4
    info "Rootfs image found at $ANDROID_MEDIA_DIR/asteroidos.ext4; mounting it now ..."
    mount -o noatime,nodiratime,sync,rw,loop $ANDROID_MEDIA_DIR/asteroidos.ext4 /loop
    [ $? -ne 0 ] || BOOT_DIR="/loop"
fi

if [ ! -e $system_partition ] && [ -n "$system_partition" ] ; then
    info "Mounting system..."
    mkdir -m 0777 $BOOT_DIR/system
    mount -t auto -o ro /dev/$system_partition $BOOT_DIR/system
    mount --bind $BOOT_DIR/system /system
fi

if [ ! -e $vendor_partition ] && [ -n "$vendor_partition" ] ; then
    info "Mounting vendor..."
    mkdir -m 0777 $BOOT_DIR/vendor
    mount -t auto -o ro /dev/$vendor_partition $BOOT_DIR/vendor
    mount --bind $BOOT_DIR/vendor /vendor
fi

if [ ! -e $firmware_partition ] && [ -n "$firmware_partition" ] ; then
    info "Mounting firmware..."
    mkdir -m 0777 $BOOT_DIR/firmware
    mount -t auto -o ro /dev/$firmware_partition $BOOT_DIR/firmware
    mount --bind $BOOT_DIR/firmware /firmware
fi

if [ -x /init.machine ]; then
    info "Run machine specific init"
    /init.machine $BOOT_DIR > /dev/kmsg 2>&1 || true
fi

setup_devtmpfs $BOOT_DIR

# The rootfs adbd unit has ConditionPathExists=/etc/usb-debugging-enabled and
# nothing else creates that file. Enabling adb unconditionally is a bring-up
# convenience to revisit before a release image.
if [ -d "$BOOT_DIR/etc" ]; then
    : > "$BOOT_DIR/etc/usb-debugging-enabled" 2>/dev/null \
        && info "rootfs: enabled userland adbd (/etc/usb-debugging-enabled)" \
        || info "rootfs: could NOT create /etc/usb-debugging-enabled"
fi

# --- release USB so userland's usb-moded stack can own it ----------------------
# init_gfs tears down whatever gadget the initramfs left. If our adbd still
# holds the UDC, that teardown blocks in D state at
#     init_gfs: unbinding 4e00000.dwc3 from /config/usb_gadget/adb/UDC
# sysinit never completes and the watch is unreachable. Aurora's initramfs does
# not keep adbd running, so it never hits this.
# Order: unbind the UDC, stop adbd, then unmount functionfs.
if [ -w /sys/kernel/config/usb_gadget/adb/UDC ]; then
    echo "" > /sys/kernel/config/usb_gadget/adb/UDC 2>/dev/null \
        && info "usb: unbound UDC from the initramfs gadget"
fi
for p in $(pidof adbd 2>/dev/null); do kill "$p" 2>/dev/null; done
if mountpoint -q /dev/usb-ffs/adb 2>/dev/null; then
    umount /dev/usb-ffs/adb 2>/dev/null || umount -l /dev/usb-ffs/adb 2>/dev/null
fi
info "usb: handed over to the rootfs (init_gfs/usb-moded own the gadget now)"

info "Move the /proc and /sys filesystems..."
umount -l /proc
umount -l /sys
mount -t proc proc $BOOT_DIR/proc
mount -t sysfs sys $BOOT_DIR/sys
mount -t tmpfs run $BOOT_DIR/run

# Tell psplash where its FIFO lives under the new root, then wait for that path
# to exist rather than sleeping a magic second. switch_root pulls the initramfs
# out from under psplash; if the path is not there yet the splash dies and the
# screen goes black across handover.
echo FIFO $BOOT_DIR/run > /run/psplash_fifo
i=0
while [ ! -e "$BOOT_DIR/run/psplash_fifo" ] && [ $i -lt 20 ]; do
    sleep 0.1
    i=$((i+1))
done

# Safety net: without systemd in BOOT_DIR, switch_root would fail, PID 1 would
# exit and the kernel would panic. Hold in the initramfs instead.
if [ ! -x "$BOOT_DIR/lib/systemd/systemd" ]; then
    info "FATAL: $BOOT_DIR/lib/systemd/systemd missing or not executable!"
    info "BOOT_DIR=$BOOT_DIR  -- staying in initramfs for adb debugging."
    info "Contents of $BOOT_DIR: $(ls -la $BOOT_DIR 2>&1)"
    while true; do sleep 60; info "init: waiting for adb (rootfs not switchable)"; done
fi

info "Switching to rootfs..."
exec switch_root -c /dev/console $BOOT_DIR /lib/systemd/systemd
