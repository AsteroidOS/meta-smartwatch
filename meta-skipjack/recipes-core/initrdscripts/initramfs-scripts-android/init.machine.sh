#!/bin/sh

echo QUIT > /run/psplash_fifo

/usr/bin/psplash --angle $rotation --no-console-switch &

sleep 1

/usr/bin/msm-fb-refresher
