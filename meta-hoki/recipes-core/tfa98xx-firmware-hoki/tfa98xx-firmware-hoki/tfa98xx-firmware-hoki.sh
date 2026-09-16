#!/bin/sh
# SPDX-License-Identifier: MIT
# Copy the real vendor tfa98xx amp DSP firmware container from the stock
# Android /system partition into the path the kernel driver's
# request_firmware() call actually looks under. Without this the amp's
# fw-state stays "Stopped" and no calibrated audio path is available.

mkdir -p /lib/firmware

if [ -e /system/etc/firmware/tfa98xx.cnt ] && [ ! -e /lib/firmware/tfa98xx.cnt ]; then
	cp /system/etc/firmware/tfa98xx.cnt /lib/firmware/tfa98xx.cnt
fi
