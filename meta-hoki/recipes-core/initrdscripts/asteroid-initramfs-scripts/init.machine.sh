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
    echo Fossil > /sys/kernel/config/usb_gadget/g1/strings/0x409/manufacturer
    echo Fossil Gen 6 > /sys/kernel/config/usb_gadget/g1/strings/0x409/product

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
# On this watch the middle button has a type of 0x74
# and the bottom button is 0x10a
# The top button is encoded in a different event.
waitmidbutton() {
    fmtstring='1/4 "%d." 1/4 "%d\t" 1/2 "%2x\t" 1/2 "%2x\t" 1/4 "%u\n"'
    result=$(hexdump -n16 -e "${fmtstring}" /dev/input/event0 | cut -f3 -)
    if [ ${result} == 74 ] ; then kill -s USR1 $1 ; fi
}

# Check if the AsteroidOS specific machine configuration file exists.
# If it doesn't then we know that the userdata partition is mounted but doesn't contain a valid AsteroidOS root.
if [ ! -e $BOOT_DIR/etc/asteroid/machine.conf ] ; then
    startadbd
else
# wait for either 1 second or for the user to hit the middle button
# If the timout expires, the watch boots normally; otherwise it starts ADB daemon
    trap "startadbd" SIGUSR1
    waitmidbutton $$ &
    sleep 1
fi
