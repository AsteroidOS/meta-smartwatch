#!/bin/sh

BOOT_DIR=$1

if [ ! -e $BOOT_DIR/system/build.prop ] ; then
    mkdir -p /tmp/vendor
    mkdir -p /tmp/system
    mount -t ext4 -o ro /dev/mmcblk0p31 /tmp/vendor
    mount -t ext4 -o ro /dev/mmcblk0p22 /tmp/system

    cp /tmp/vendor/firmware/nanohub.full.*.bin $BOOT_DIR/system/vendor/firmware/
    cp /tmp/vendor/build.prop $BOOT_DIR/system/vendor/
    cp /tmp/system/build.prop $BOOT_DIR/system/
fi
