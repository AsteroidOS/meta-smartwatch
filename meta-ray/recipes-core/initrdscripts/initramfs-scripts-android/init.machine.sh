#!/bin/sh

echo QUIT > /run/psplash_fifo

/usr/bin/psplash --angle $rotation --no-console-switch &

BOOT_DIR=$1

sleep 1

/usr/bin/msm-fb-refresher
