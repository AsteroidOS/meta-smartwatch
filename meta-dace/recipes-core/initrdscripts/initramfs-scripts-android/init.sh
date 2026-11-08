#! /bin/sh

. /machine.conf

# Log to /dev/kmsg (printk ring buffer) rather than /dev/ttyprintk: this kernel
# has CONFIG_TTY_PRINTK=n (gki_defconfig default).
info() { echo "init: $1" > /dev/kmsg 2>/dev/null; }
fail() {
    echo "init: Failed" > /dev/kmsg 2>/dev/null
    echo "init: $1" > /dev/kmsg 2>/dev/null
    echo "init: Waiting for 15 seconds before rebooting ..." > /dev/kmsg 2>/dev/null
    sleep 15s; reboot -f
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

KREL=$(uname -r)
[ ! -e "/lib/modules/$KREL" ] && ln -sf . "/lib/modules/$KREL"

# The DT default force-disables USB at boot, which keeps dwc3-msm from
# registering a UDC (no UDC, no adb). The busybox modprobe in this initramfs
# does not honour /etc/modprobe.d, so pass the parameter directly.
modprobe google-extcon-usb-shim usb_force_disable_boot=0 2>/dev/kmsg

info "Loading dace kernel modules from /etc/modules.load.dace ..."
while read mod; do
    case "$mod" in ''|\#*) continue ;; esac
    name="${mod%.ko}"
    # qnoc-monaco, msm_drm, msm_kgsl and msm_geni_serial need the apps-smmu to
    # be registered first to obtain an IOMMU domain, and the SMMU only
    # registers once qnoc-monaco has exposed the ICC provider. Load them
    # explicitly in order below.
    case "$name" in
        qnoc-monaco|msm_drm|msm_kgsl|msm_geni_serial)
            info "M-- ${name} (deferred)"
            continue ;;
    esac
    modprobe "$name" 2>/dev/kmsg
done < /etc/modules.load.dace

# Open the USB gate in case the shim latched it via DT before our param applied.
for fd in /sys/devices/platform/soc/soc:extcon_usb_shim/force_disable \
          /sys/bus/platform/devices/soc:extcon_usb_shim/force_disable; do
    [ -e "$fd" ] && echo 0 > "$fd" 2>/dev/kmsg
done
mount -t debugfs none /sys/kernel/debug 2>/dev/null || true

# Display bring-up order: qnoc-monaco -> apps-smmu -> (msm_drm, msm_kgsl).
# msm_smmu_probe() returns -EINVAL (not -EPROBE_DEFER) if the SMMU has no
# domain yet, so msm_drm must load after the SMMU is up.
modprobe qnoc-monaco 2>/dev/kmsg
# msm_geni_serial needs the qnoc ICC nodes registered for geni_icc_get().
modprobe msm_geni_serial 2>/dev/kmsg

# Wait (bounded) for the QUP IOMMU domains; dace-iommu-defer.patch makes the
# QUP wrapper and GPI wait for the apps-smmu instead of using physical
# addresses.
_i=0
while [ $_i -lt 15 ]; do
    [ -e /sys/bus/platform/devices/4ac0000.qcom,qupv3_0_geni_se/iommu_group ] && \
    [ -e /sys/bus/platform/devices/4a00000.qcom,gpi-dma/iommu_group ] && break
    _i=$((_i+1)); sleep 1
done
info "QUP IOMMU domains ready after ${_i}s"

modprobe msm_drm 2>/dev/kmsg
modprobe msm_kgsl 2>/dev/kmsg

info "Mounting sdcard..."
mkdir -m 0777 /sdcard /loop
w=0
while [ ! -e /dev/$sdcard_partition ] && [ $w -lt 30 ]; do
    info "Waiting for $sdcard_partition..."
    sleep 1
    w=$((w+1))
done

FSTYPE=${sdcard_fstype:-auto}
if [ -e /dev/$sdcard_partition ]; then
    # The userdata partition is F2FS; never run e2fsck on it.
    mount -t $FSTYPE -o rw,noatime,nodiratime /dev/$sdcard_partition /sdcard
    [ $? -eq 0 ] || fail "Failed to mount the sdcard. Cannot continue."
else
    fail "No $sdcard_partition after 30s"
fi

ANDROID_MEDIA_DIR="/sdcard"
[ -d /sdcard/media/0 ] && ANDROID_MEDIA_DIR="/sdcard/media/0"
[ -e /sdcard/asteroidos.ext4 ] && ANDROID_MEDIA_DIR="/sdcard"

BOOT_DIR="/sdcard"
if [ -e $ANDROID_MEDIA_DIR/asteroidos.ext4 ] ; then
    /sbin/fsck.ext4 -p $ANDROID_MEDIA_DIR/asteroidos.ext4 2>/dev/null
    mount -o noatime,nodiratime,rw,loop $ANDROID_MEDIA_DIR/asteroidos.ext4 /loop
    [ $? -ne 0 ] || BOOT_DIR="/loop"
fi

# dace fixups applied to the rootfs before systemd starts.
if [ -x "$BOOT_DIR/lib/systemd/systemd" ]; then
    # zinitix must not be loaded by udev on coldplug: the vendor Zinitix driver
    # is loaded explicitly from dace-lxc-hal-start.sh after the display HALs.
    BL="$BOOT_DIR/etc/modprobe.d/00-dace-vendor-blacklist.conf"
    if ! grep -qs '^blacklist zinitix' "$BL"; then
        mkdir -p "$BOOT_DIR/etc/modprobe.d"
        printf '\nblacklist zinitix\n' >> "$BL"
    fi
    # The T5 has no functional ambient light sensor; with the ALS filter mce
    # falls back to the darkest profile.
    if [ -f "$BOOT_DIR/etc/mce/10mce.ini" ] && grep -q "filter-brightness-als" "$BOOT_DIR/etc/mce/10mce.ini"; then
        sed -i 's/filter-brightness-als;//; s/;filter-brightness-als//' "$BOOT_DIR/etc/mce/10mce.ini"
    fi
    # sensorfwd crash-loops on this device and its bogus proximity events hold
    # a wakelock that prevents autosleep.
    ln -sf /dev/null "$BOOT_DIR/etc/systemd/system/sensorfwd.service"
    # Unmask the USB units in case a previous boot masked them.
    for u in init_gfs.service usb-moded.service android-tools-adbd.service adbd-prepare.service dace-lxc-android.service; do
        f="$BOOT_DIR/etc/systemd/system/$u"
        if [ -L "$f" ] && [ "$(readlink $f 2>/dev/null)" = "/dev/null" ]; then
            rm -f "$f"
        fi
    done
    # usb-moded needs g1 to exist (init_gfs creates it) and must assume the
    # cable is connected (smblite is not up yet in the host boot).
    mkdir -p "$BOOT_DIR/usr/lib/systemd/system/sysinit.target.wants"
    ln -sf ../init_gfs.service \
        "$BOOT_DIR/usr/lib/systemd/system/sysinit.target.wants/init_gfs.service"
    mkdir -p $BOOT_DIR/etc/systemd/system/usb-moded.service.d
    printf '%s\n' '[Service]' 'Environment=USB_MODED_ARGS=-f -D' \
        > $BOOT_DIR/etc/systemd/system/usb-moded.service.d/10-dace-fallback.conf
fi

if [ -x /init.machine ]; then
    /init.machine $BOOT_DIR > /dev/kmsg 2>&1 || true
fi

setup_devtmpfs $BOOT_DIR

info "Move the /proc and /sys filesystems..."
umount -l /proc 2>/dev/null
umount -l /sys 2>/dev/null
mount -t proc proc $BOOT_DIR/proc 2>/dev/null
mount -t sysfs sys $BOOT_DIR/sys 2>/dev/null
mount -t tmpfs run $BOOT_DIR/run 2>/dev/null

echo "FIFO $BOOT_DIR/run" > /run/psplash_fifo 2>/dev/null

if [ ! -x "$BOOT_DIR/lib/systemd/systemd" ]; then
    info "FATAL: $BOOT_DIR/lib/systemd/systemd missing or not executable!"
    info "BOOT_DIR=$BOOT_DIR -- staying in initramfs for adb debugging."
else
    info "Switching to rootfs..."
    exec switch_root -c /dev/console $BOOT_DIR /lib/systemd/systemd
fi

# No rootfs: bring up adb from the initramfs so the watch can be recovered.
info "No rootfs -- starting adb from ramfs"
mkdir -p /sys/kernel/config
mount -t configfs none /sys/kernel/config 2>/dev/null
for _u in /sys/kernel/config/usb_gadget/*/UDC; do
    [ -e "$_u" ] || continue
    echo "" > "$_u" 2>/dev/null
done
sleep 1
/usr/bin/android-gadget-setup adb 2>/dev/null
echo 18d1 > /sys/class/android_usb/android0/idVendor 2>/dev/null
echo d002 > /sys/class/android_usb/android0/idProduct 2>/dev/null
echo adb  > /sys/class/android_usb/android0/f_ffs/aliases 2>/dev/null
echo ffs  > /sys/class/android_usb/android0/functions 2>/dev/null
echo 1    > /sys/class/android_usb/android0/enable 2>/dev/null
/usr/bin/adbd 2>/dev/null &

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
    echo "$UDC" > /sys/kernel/config/usb_gadget/adb/UDC 2>/dev/null
    info "adb bound to UDC=$UDC"
else
    info "no UDC after 30s"
fi

while true; do sleep 3600; done
