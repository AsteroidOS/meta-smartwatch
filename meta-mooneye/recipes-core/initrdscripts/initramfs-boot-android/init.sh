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

# Checks wether we need to start adbd for interactive debugging
cat /proc/cmdline | grep debug-ramdisk
if [ $? -ne 1 ] ; then
    mkdir -p /config
    mount -t configfs none /config

    mkdir -p /config/usb_gadget/g1
    echo 0x0e8d > /config/usb_gadget/g1/idVendor 
    echo 0x0010 > /config/usb_gadget/g1/idProduct

    mkdir /config/usb_gadget/g1/strings/0x409
    serial="$(cat /proc/cmdline | sed 's/.*androidboot.serialno=//' | sed 's/ .*//')"
    echo $serial > /config/usb_gadget/g1/strings/0x409/serialnumber 
    echo AsteroidOS > /config/usb_gadget/g1/strings/0x409/manufacturer
    echo InitRamDisk > /config/usb_gadget/g1/strings/0x409/product

    mkdir /config/usb_gadget/g1/configs/c.1
    mkdir /config/usb_gadget/g1/configs/c.1/strings/0x409
    echo adb > /config/usb_gadget/g1/configs/c.1/strings/0x409/configuration
    echo 500 > /config/usb_gadget/g1/configs/c.1/MaxPower

    mkdir /config/usb_gadget/g1/functions/ffs.adb
    ln -s /config/usb_gadget/g1/functions/ffs.adb /config/usb_gadget/g1/configs/c.1/ffs.adb

    mkdir -p /dev/usb-ffs/adb
    mount -t functionfs adb /dev/usb-ffs/adb

    /usr/bin/adbd &

    sleep 4
    echo "musb-hdrc.0.auto" > /config/usb_gadget/g1/UDC
    sleep 10000000
fi

rotation=0
if [ -e /etc/rotation ]; then
    read rotation < /etc/rotation
fi

echo "U:400x400p-0" > /sys/class/graphics/fb0/mode
/usr/bin/psplash --angle $rotation --no-console-switch &

# The sdcard partition may be the rootfs itself or contain a loop file
info "Mounting sdcard..."
mkdir -m 0777 /sdcard /loop

while [ ! -e /sys/block/mmcblk0 ] ; do
    info "Waiting for mmcblk0..."
    sleep 1
done

/sbin/fsck.ext4 -p /dev/$sdcard_partition
mount -t auto -o rw,noatime,nodiratime,nodelalloc /dev/$sdcard_partition /sdcard
[ $? -eq 0 ] || fail "Failed to mount the sdcard. Cannot continue."

info "Checking for loop rootfs image on the sdcard..."
ANDROID_MEDIA_DIR="/sdcard/media/"
if [ -d /sdcard/media/0 ] ; then
    ANDROID_MEDIA_DIR="/sdcard/media/0"
fi

BOOT_DIR="/sdcard"
if [ -e $ANDROID_MEDIA_DIR/asteroidos.ext4 ] ; then
    # Boots from a /sdcard/asteroidos.ext4 loop file
    info "Rootfs image found at $ANDROID_MEDIA_DIR/asteroidos.ext4; mounting it now ..."
    mount -o noatime,nodiratime,nodelalloc,sync,rw,loop $ANDROID_MEDIA_DIR/asteroidos.ext4 /loop
    [ $? -ne 0 ] || BOOT_DIR="/loop"
fi

setup_devtmpfs $BOOT_DIR

info "Move the /proc and /sys filesystems..."
umount -l /proc
umount -l /sys
mount -t proc proc $BOOT_DIR/proc
mount -t sysfs sys $BOOT_DIR/sys

info "Switching to rootfs..."
exec switch_root -c /dev/ttyprintk $BOOT_DIR /lib/systemd/systemd
