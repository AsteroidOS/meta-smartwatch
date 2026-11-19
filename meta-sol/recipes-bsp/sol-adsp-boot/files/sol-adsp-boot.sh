#!/bin/sh
# /usr/libexec/sol-adsp-boot.sh
#
# Boot sol's ADSP, where the Qualcomm sensor sub-HAL runs.
#
# The AIDL multihal's /vendor/etc/sensors/hals.conf names one sub-HAL,
# /vendor/lib/hw/sensors.usf.so, which runs on the ADSP as the "sensorspd"
# protection domain. With the ADSP down, sscrpcd loops trying to spawn that PD
# and no sensor enumerates. (The Google MCU leg, nanohub -> mcu_mgmtd -> CHRE,
# comes up via spi-msm-geni, see sol-post-rootfs.conf.)
#
# remoteproc0 stays offline because the ADSP firmware
# (/android/vendor/firmware/adsp.mdt + adsp.bNN) is not on the loader's path.
# Stock starts it from second-stage init. Once running, dmesg matches stock:
#     adsprpc: fastrpc_rpmsg_probe: opened rpmsg channel for lpass
set -u

RPROC=/sys/class/remoteproc/remoteproc0
FWDIR=/android/vendor/firmware
FWPATH=/sys/module/firmware_class/parameters/path

[ -d "$RPROC" ] || { echo "sol-adsp-boot: $RPROC absent, nothing to do"; exit 0; }

name=$(cat "$RPROC/name" 2>/dev/null)
case "$name" in
    *remoteproc-adsp) : ;;
    *) echo "sol-adsp-boot: remoteproc0 is '$name', not the adsp -- refusing to start it"; exit 0 ;;
esac

state=$(cat "$RPROC/state" 2>/dev/null)
[ "$state" = "running" ] && { echo "sol-adsp-boot: already running"; exit 0; }

if [ ! -f "$FWDIR/adsp.mdt" ]; then
    echo "sol-adsp-boot: $FWDIR/adsp.mdt missing -- is sol-vendor-mount up?"
    exit 0
fi

# Additional search path for the firmware loader; the kernel tries this before
# falling back to /lib/firmware, so the GPU firmware there keeps working.
echo "$FWDIR" > "$FWPATH" 2>/dev/null || echo "sol-adsp-boot: could not set $FWPATH"

echo start > "$RPROC/state" 2>/dev/null
sleep 3
echo "sol-adsp-boot: remoteproc0 state=$(cat "$RPROC/state" 2>/dev/null)"
