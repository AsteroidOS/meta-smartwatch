#!/bin/sh
# SPDX-License-Identifier: MIT
# Symlink stock vendor WCNSS firmware/calibration data into the path the
# out-of-tree pronto WLAN driver actually looks under, power up WCNSS by
# opening (not writing) /dev/wcnss_wlan, then load the driver.
# See https://github.com/AsteroidOS/meta-smartwatch/issues/224

mkdir -p /lib/firmware/wlan/prima

for f in WCNSS_qcom_cfg.ini WCNSS_qcom_wlan_nv.bin WCNSS_wlan_dictionary.dat; do
	if [ ! -e "/lib/firmware/wlan/prima/$f" ]; then
		ln -s "/vendor/firmware/wlan/prima/$f" "/lib/firmware/wlan/prima/$f"
	fi
done

# Opening this device node is what actually powers WCNSS up; the write
# itself is expected to fail and is not an error.
echo 1 > /dev/wcnss_wlan 2>/dev/null || true

modprobe wlan
