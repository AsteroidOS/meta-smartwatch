#!/bin/sh

BOOT_DIR=$1

# Manually mount the system partition as it contains the boot image (ramdisk) as well as the system data (under /system/system).
mkdir -m 0777 $BOOT_DIR/android-root
mount -t auto -o ro /dev/mmcblk0p29 $BOOT_DIR/android-root

# Allow Android binaries to load libraries from /usr/libexec/ (for example /usr/libexec/hal-droid/system/lib/libselinux_stubs.so)
mount --bind /ld.config.28.txt $BOOT_DIR/android-root/system/etc/ld.config.28.txt

# Make the 'system' folder available as the system partition to the rootfs.
ln -s /android-root/system/ $BOOT_DIR/system

# Check if the AsteroidOS specific machine configuration file exists.
# If it doesn't then we know that the userdata partition is mounted but doesn't contain a valid AsteroidOS root.
if [ ! -e $BOOT_DIR/etc/asteroid/machine.conf ] ; then
    echo MSG "Push asteroidos.ext4 via ADB" > /run/psplash_fifo

    # Setup for use with ConfigFS
    /usr/bin/android-gadget-setup adb

    # Correct properties as /system/ may not exist.
    serial="$(cat /proc/cmdline | sed 's/.*androidboot.serialno=//' | sed 's/ .*//')"
    echo $serial > /sys/kernel/config/usb_gadget/g1/strings/0x409/serialnumber
    echo Mobvoi > /sys/kernel/config/usb_gadget/g1/strings/0x409/manufacturer
    echo TicWatch Pro 3 > /sys/kernel/config/usb_gadget/g1/strings/0x409/product

    /usr/bin/adbd
fi
