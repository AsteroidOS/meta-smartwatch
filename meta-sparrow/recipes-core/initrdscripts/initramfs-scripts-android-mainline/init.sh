#! /bin/sh

# machine.conf should provide $sdcard_partition
. /machine.conf

# Logging functions
info() {
    echo "$1" > /dev/ttyprintk
}

fail() {
    echo "Failed" > /dev/ttyprintk
    echo "$1" > /dev/ttyprintk
    echo "Waiting for 15 seconds before rebooting ..." > /dev/ttyprintk
    sleep 15s
    reboot
}

# Populates /dev (called for /dev and /rfs/dev)
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
mkdir -m 0755 /proc
mount -t proc proc /proc
mkdir -m 0755 /sys
mount -t sysfs sys /sys
mkdir -p /dev
setup_devtmpfs ""

# Checks wether we need to start telnet for interactive debugging
cat /proc/cmdline | grep debug-ramdisk
if [ $? -ne 1 ] ; then
    mount -t configfs -o nodev,noexec,nosuid configfs /sys/kernel/config

    CONFIGFS=/sys/kernel/config/usb_gadget
    mkdir $CONFIGFS/g1
    echo 0x18D1 > $CONFIGFS/g1/idVendor
    echo 0xD001 > $CONFIGFS/g1/idProduct

    mkdir $CONFIGFS/g1/strings/0x409

    echo 0123456789 > $CONFIGFS/g1/strings/0x409/manufacturer
    echo AsteroidOS > $CONFIGFS/g1/strings/0x409/serialnumber
    echo InitRamDisk > $CONFIGFS/g1/strings/0x409/product

    mkdir $CONFIGFS/g1/functions/rndis.usb0
    mkdir $CONFIGFS/g1/configs/c.1
    mkdir $CONFIGFS/g1/configs/c.1/strings/0x409
    echo "rndis" > $CONFIGFS/g1/configs/c.1/strings/0x409/configuration

    ln -s $CONFIGFS/g1/functions/rndis.usb0 $CONFIGFS/g1/configs/c.1
    echo "$(ls /sys/class/udc)" > $CONFIGFS/g1/UDC

    ifconfig usb0 192.168.2.15
    # On the computer, use # ifconfig theUsbInterface 192.168.2.1

    telnetd -F -l /bin/sh
fi

rotation=0
if [ -e /etc/rotation ]; then
    read rotation < /etc/rotation
fi

if [ -x /usr/bin/msm-fb-refresher ] ; then
    /usr/bin/msm-fb-refresher
fi

/usr/bin/psplash --angle $rotation --no-console-switch &

# The sdcard partition may be the rootfs itself or contain a loop file
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
if [ -d /sdcard/media/0 ] ; then
    ANDROID_MEDIA_DIR="/sdcard/media/0"
fi

BOOT_DIR="/sdcard"
if [ -e $ANDROID_MEDIA_DIR/asteroidos.ext4 ] ; then
    /sbin/fsck.ext4 -p $ANDROID_MEDIA_DIR/asteroidos.ext4
    # Boots from a /sdcard/asteroidos.ext4 loop file
    info "Rootfs image found at $ANDROID_MEDIA_DIR/asteroidos.ext4; mounting it now ..."
    mount -o noatime,nodiratime,sync,rw,loop $ANDROID_MEDIA_DIR/asteroidos.ext4 /loop
    [ $? -ne 0 ] || BOOT_DIR="/loop"
fi

if [ ! -e $system_partition ] ; then
    info "Mounting system..."
    mkdir -m 0777 $BOOT_DIR/system
    mount -t auto -o ro /dev/$system_partition $BOOT_DIR/system
    mount --bind $BOOT_DIR/system /system
fi

if [ ! -e $vendor_partition ] ; then
    info "Mounting vendor..."
    mkdir -m 0777 $BOOT_DIR/vendor
    mount -t auto -o ro /dev/$vendor_partition $BOOT_DIR/vendor
    mount --bind $BOOT_DIR/vendor /vendor
fi

if [ ! -e $firmware_partition ] ; then
    info "Mounting firmware..."
    mkdir -m 0777 $BOOT_DIR/firmware
    mount -t auto -o ro /dev/$firmware_partition $BOOT_DIR/firmware
    mount --bind $BOOT_DIR/firmware /firmware
fi

if [ -x /init.machine ]; then
    info "Run machine specific init"
    /init.machine $BOOT_DIR > /dev/ttyprintk
fi

setup_devtmpfs $BOOT_DIR

info "Move the /proc and /sys filesystems..."
umount -l /proc
umount -l /sys
mount -t proc proc $BOOT_DIR/proc
mount -t sysfs sys $BOOT_DIR/sys
mount -t tmpfs run $BOOT_DIR/run

echo FIFO $BOOT_DIR/run > /run/psplash_fifo
# We need to give psplash time to create the new named pipe.
sleep 1

info "Switching to rootfs..."
exec switch_root -c /dev/ttyprintk $BOOT_DIR /lib/systemd/systemd

