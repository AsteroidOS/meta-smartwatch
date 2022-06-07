#!/bin/sh

BOOT_DIR=$1

mkdir $BOOT_DIR/nvdata
mount -t ext4 /dev/mmcblk0p10 $BOOT_DIR/nvdata

mkdir -p /nvdata
mkdir -p /system/etc/wifi
mkdir -p /system/vendor/firmware/
mkdir -p /vendor/firmware

mount --bind $BOOT_DIR/system/etc/wifi          /system/etc/wifi
mount --bind $BOOT_DIR/system/vendor/firmware/  /system/vendor/firmware/
mount --bind $BOOT_DIR/vendor/firmware          /vendor/firmware
mount --bind $BOOT_DIR/nvdata                   /nvdata

dd if=/nvdata/MACWLAN of=/proc/wifi/mac

ip link set up dev wlan0
sleep 1
ip link set down dev wlan0
