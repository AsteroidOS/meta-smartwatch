#! /bin/sh

. /machine.conf

# Log to /dev/kmsg (printk ring buffer) rather than /dev/ttyprintk:
# this kernel has CONFIG_TTY_PRINTK=n (gki_defconfig default), so the
# ttyprintk node never exists and every info() was silently dropped.
# /dev/kmsg is provided by CONFIG_PRINTK + printk.devkmsg=on (set on
# our cmdline), so it's available as soon as devtmpfs is mounted.
info() { echo "init: $1" > /dev/kmsg 2>/dev/null; }
fail() {
    echo "init: Failed" > /dev/kmsg 2>/dev/null
    echo "init: $1" > /dev/kmsg 2>/dev/null
    echo "init: Waiting for 15 seconds before rebooting ..." > /dev/kmsg 2>/dev/null
    sleep 15s; reboot
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

# ---- AURORA-SPECIFIC: load the vendor module chain ----
# Our 85+ SoC modules are =m. Load them now so /dev/mmcblk0pN appears,
# the WLAN/sensor/audio drivers are ready, etc.
#
# vendor_kernel_boot ships .ko's at a FLAT /lib/modules/*.ko (no
# $(uname -r) subdirectory). busybox modprobe DOES expect /lib/modules/$(uname
# -r)/{modules.dep,*.ko}, so we symlink the version subdir to the flat root.
# modules.dep is already at /lib/modules/modules.dep (shipped by VKB) and
# contains absolute paths like "/lib/modules/clk-qcom.ko" -- those resolve
# directly, the symlink only exists so modprobe's chdir() lands in the right
# place to load modules.dep.
#
# With modprobe + modules.dep, dep order in modules.load.aurora is IRRELEVANT
# -- modprobe walks the dep tree per module. This matters because hand-ordering
# 96 modules across all SoC subsystems is fragile.
KREL=$(uname -r)
[ ! -e "/lib/modules/$KREL" ] && ln -sf . "/lib/modules/$KREL"

# Pre-load the google-extcon-usb-shim with usb_force_disable_boot=0 BEFORE the
# bulk modprobe loop. The DT default has the shim force-disabling USB at boot
# ("disable-param:0x1" in dmesg) which keeps dwc3-msm from registering a UDC --
# no UDC, no adb. We can't rely on /etc/modprobe.d/*.conf because the busybox
# modprobe in our initramfs doesn't honor it. Passing the param directly to
# modprobe IS honored.
info "Pre-loading google-extcon-usb-shim with USB gate forced open ..."
modprobe google-extcon-usb-shim usb_force_disable_boot=0 2>/dev/kmsg

info "Loading aurora kernel modules from /etc/modules.load.aurora ..."
while read mod; do
    case "$mod" in ''|\#*) continue ;; esac
    modprobe "${mod%.ko}" 2>/dev/kmsg
done < /etc/modules.load.aurora
info "Loaded aurora kernel modules: $(ls /sys/bus/platform/drivers/ 2>/dev/null | wc -l) platform drivers registered"

# Belt-and-braces: even with usb_force_disable_boot=0 the shim may have latched
# the gate via DT before our param applied. The runtime sysfs attr
# `force_disable` is changeable -- write 0 to flip USB on.
for fd in /sys/devices/platform/soc/soc:extcon_usb_shim/force_disable \
          /sys/bus/platform/devices/soc:extcon_usb_shim/force_disable; do
    [ -e "$fd" ] && echo 0 > "$fd" 2>/dev/kmsg && info "USB gate opened via $fd"
done

# Cache cmdline contents while /proc is still mounted. The proc/sys move
# (umount -l /proc, mount -t proc proc $BOOT_DIR/proc) happens before
# switch_root, so any LATER `grep /proc/cmdline` would fail.
CMDLINE=$(cat /proc/cmdline 2>/dev/null)

# Checks whether we need to start adbd for interactive debugging
case "$CMDLINE" in *debug-ramdisk*) DEBUG_RAMDISK=1 ;; *) DEBUG_RAMDISK=0 ;; esac
if [ "$DEBUG_RAMDISK" = "1" ]; then
    # On this 5.15 GKI kernel CONFIG_USB_F_FS=y but functionfs_init() is NOT a
    # fs_initcall -- f_fs.c's filesystem registration happens lazily when
    # _ffs_alloc_dev sees its first device (configfs mkdir functions/ffs.X). So
    # `mount -t functionfs` only works AFTER we've created an ffs.X function
    # via configfs.
    #
    # android-gadget-setup adb does that for us, but its first line is `cd
    # /sys/kernel/config/usb_gadget` so it REQUIRES configfs to be mounted.
    # systemd auto-mounts it in the rootfs but we're pre- systemd in the
    # initramfs -- do it ourselves.
    mkdir -p /sys/kernel/config
    mount -t configfs none /sys/kernel/config 2>/dev/null || true

    # android-gadget-setup adb creates the configfs gadget, the
    # functions/ffs.usb0 function (which triggers functionfs_init() and
    # registers the `functionfs` fs type), AND mounts it at /dev/usb-ffs/adb.
    # We don't need a separate mount call.
    /usr/bin/android-gadget-setup adb

    # Legacy /sys/class/android_usb writes -- silently no-op on our 5.15 GKI
    # kernel which doesn't have CONFIG_USB_ANDROID. Kept for parity with the
    # upstream init.sh.
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

    # Bind to the first available UDC -- writing the UDC name into
    # configfs/usb_gadget/<g>/UDC is what makes the USB device visible to the
    # host. dwc3-msm probes via deferred-init module load (it's =m, loaded from
    # modules.load.aurora after sdhci-msm), so the UDC appears 2-10s into boot.
    # Poll up to 30s in 0.5s steps and verify an entry actually exists in
    # /sys/class/udc/.
    info "adbd: usb_gadget=$(cd /sys/kernel/config/usb_gadget 2>/dev/null && echo *)"
    UDC=""
    i=0
    while [ $i -lt 30 ]; do
        UDC=$(cd /sys/class/udc 2>/dev/null && echo *)
        case "$UDC" in '*'|''|'.'|'..') UDC="" ;; esac
        [ -n "$UDC" ] && break
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
        info "adbd: NO UDC found after 30s -- dwc3-msm gate still off?"
        info "adbd: /sys/class/udc entries = '$(cd /sys/class/udc 2>/dev/null && echo *)'"
    fi

    # debug-ramdisk is a sticky mode: stay in the initramfs forever with adb up.
    # The user flips it on to investigate boot failures and doesn't want
    # switch_root to free our adbd out from under them.
    info "debug-ramdisk: staying in initramfs (adb available); never switching to rootfs"
    while true; do sleep 3600; done
fi

rotation=0
[ -e /etc/rotation ] && read rotation < /etc/rotation
[ -x /usr/bin/msm-fb-refresher ] && /usr/bin/msm-fb-refresher
/usr/bin/psplash --angle $rotation --no-console-switch &

info "Mounting sdcard..."
mkdir -m 0777 /sdcard /loop
while [ ! -e /dev/$sdcard_partition ] ; do
    info "Waiting for $sdcard_partition..."
    sleep 1
done

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

info "Move the /proc and /sys filesystems..."
umount -l /proc
umount -l /sys
mount -t proc proc $BOOT_DIR/proc
mount -t sysfs sys $BOOT_DIR/sys
mount -t tmpfs run $BOOT_DIR/run

echo FIFO $BOOT_DIR/run > /run/psplash_fifo
sleep 1

# Safety net: if BOOT_DIR's systemd doesn't exist, don't even try
# switch_root -- it would fail exec("..."), PID 1 would exit, and the
# kernel would panic ("Attempted to kill init"). Instead, stay alive
# in the initramfs so adb (debug-ramdisk) can be used to investigate.
if [ ! -x "$BOOT_DIR/lib/systemd/systemd" ]; then
    info "FATAL: $BOOT_DIR/lib/systemd/systemd missing or not executable!"
    info "BOOT_DIR=$BOOT_DIR  -- staying in initramfs for adb debugging."
    info "Contents of $BOOT_DIR: $(ls -la $BOOT_DIR 2>&1)"
    # Don't proceed: panic-on-PID1-exit is worse than holding here.
    while true; do sleep 60; info "init: waiting for adb (rootfs not switchable)"; done
fi

info "Switching to rootfs..."
exec switch_root -c /dev/console $BOOT_DIR /lib/systemd/systemd
