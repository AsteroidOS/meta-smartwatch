#!/bin/sh

BOOT_DIR=$1

# we need that for WiFi mac address
mkdir $BOOT_DIR/persist
mount -t ext4 /dev/mmcblk0p13 $BOOT_DIR/persist

# reading other android sources, it seems to be pretty normal to
# just change permissions for /persist stuff    ¯\_(ツ)_/¯
chown -R root:root $BOOT_DIR/persist
chmod 0755 $BOOT_DIR/persist/wifi          $BOOT_DIR/persist/sensors
chmod 0644 $BOOT_DIR/persist/wifi/.macaddr $BOOT_DIR/persist/sensors/*

# make firmware, wifi config and wifi mac available to initramfs
# then reboot wifi controller to load mac address
# specific wifi and firmware only because we don't want to enforce
# a directory layout in initramfs
#
# we have to reboot the wifi controller in initramfs, so applications
# on the real system only see the wifi controller with the real mac
# - connman uses the mac address to generate names for services and
# cannot connect to previously configured ones otherwise

mkdir -p /persist
mkdir -p /system/etc/wifi
mkdir -p /vendor/firmware

mount --bind $BOOT_DIR/persist          /persist
mount --bind $BOOT_DIR/system/etc/wifi  /system/etc/wifi
mount --bind $BOOT_DIR/vendor/firmware  /vendor/firmware

ip link set up dev wlan0
sleep 1
ip link set down dev wlan0
