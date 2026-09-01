#!/bin/sh

BOOT_DIR=$1


# Start the ADB daemon
startadbd() {
    trap - SIGUSR1
    echo MSG "Push asteroidos.ext4 via ADB" > /run/psplash_fifo

    # Setup for use with ConfigFS
    /usr/bin/android-gadget-setup adb

    # Correct properties as /system/ may not exist.
    serial="$(cat /proc/cmdline | sed 's/.*androidboot.serialno=//' | sed 's/ .*//')"
    echo $serial > /sys/kernel/config/usb_gadget/g1/strings/0x409/serialnumber
    echo OPPO > /sys/kernel/config/usb_gadget/g1/strings/0x409/manufacturer
    echo OPPO Watch > /sys/kernel/config/usb_gadget/g1/strings/0x409/product

    /usr/bin/adbd
}

# wait for the middle button to be pressed, and if it is,
# kill the calling process group with SIGUSR1
# The format of these events is documented here:
# https://github.com/torvalds/linux/blob/master/include/uapi/linux/input.h#L28-L47
# u32 - tv_sec
# u32 - tv_usec
# u16 - type
# u16 - code
# s32 - value
# On this watch the bottom button has a type of 2ee
# The top button is encoded in a different event.
waitmidbutton() {
    fmtstring='1/4 "%d." 1/4 "%d\t" 1/2 "%2x\t" 1/2 "%2x\t" 1/4 "%u\n"'
    result=$(hexdump -n16 -e "${fmtstring}" /dev/input/event0 | cut -f3 -)
    if [ ${result} == 2ee ] ; then kill -s USR1 $1 ; fi
}

# Manually mount the system partition as it contains the boot image (ramdisk) as well as the system data (under /system/system).
mkdir -m 0777 $BOOT_DIR/boot
mount -t auto -o ro /dev/mmcblk0p36 $BOOT_DIR/boot

# Allow Android binaries to load libraries from /usr/libexec/ (for example /usr/libexec/hal-droid/system/lib/libselinux_stubs.so)
mount --bind /ld.config.28.txt $BOOT_DIR/boot/system/etc/ld.config.28.txt

# Make the 'system' folder available as the system partition to the rootfs.
ln -s /boot/system/ $BOOT_DIR/system

# Check if the AsteroidOS specific machine configuration file exists.
# If it doesn't then we know that the userdata partition is mounted but doesn't contain a valid AsteroidOS root.
if [ ! -e $BOOT_DIR/etc/asteroid/machine.conf ] ; then
    startadbd
else
# wait for either 5 seconds or for the user to hit the middle button
# If the timout expires, the watch boots normally; otherwise it starts ADB daemon
    trap "startadbd" SIGUSR1
    waitmidbutton $$ &
    sleep 1
fi
