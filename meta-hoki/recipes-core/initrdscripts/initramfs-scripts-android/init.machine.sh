#!/bin/sh

BOOT_DIR=$1

# Check if the AsteroidOS specific machine configuration file exists.
# If it doesn't then we know that the userdata partition is mounted but doesn't contain a valid AsteroidOS root.
if [ ! -e $BOOT_DIR/etc/asteroid/machine.conf ] ; then
    echo MSG "Push asteroidos.ext4 via ADB" > /run/psplash_fifo

    # Setup for use with ConfigFS
    /usr/bin/android-gadget-setup adb

    # Correct properties as /system/ may not exist.
    serial="$(cat /proc/cmdline | sed 's/.*androidboot.serialno=//' | sed 's/ .*//')"
    echo $serial > /sys/kernel/config/usb_gadget/g1/strings/0x409/serialnumber
    echo Fossil > /sys/kernel/config/usb_gadget/g1/strings/0x409/manufacturer
    echo Fossil Gen 6 > /sys/kernel/config/usb_gadget/g1/strings/0x409/product

    /usr/bin/adbd
fi
