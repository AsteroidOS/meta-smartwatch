#!/bin/sh
# dace: force the USB SDP current so smblite CHARGES.
#
# ROOT CAUSE (measured 2026-09-25): smblite starts with `usb_icl_votable` at
# ~2 mA (the `SW_ICL_MAX_VOTER`/the Android USB driver which does not run here:
# there is no Android framework). `smblite_lib_configure_usb_icl()` suspends the
# input if the ICL <= 25 uA -> POWER_PATH_STATUS=0x29 (USE_USBIN=1 but
# USBIN_SUSPEND=1 and VALID_INPUT=0) -> `usb/online=0` -> `battery Discharging`,
# the watch does NOT charge (even though the bootloader does charge on the same
# port).
#
# The Android USB driver does the same via
# POWER_SUPPLY_PROP_INPUT_CURRENT_LIMIT; we replicate that step: on detecting USB
# present with online=0, we write 500000 uA (SDP_CURRENT_MAX) to
# `usb/input_current_limit` -> `smblite_lib_set_prop_current_max()` votes
# `USB_PSY_VOTER` and clears `SW_ICL_MAX_VOTER` -> online=1 and `battery
# Charging` (measured: current_now becomes +354 mA).
#
# Only touches SDP ports (POWER_SUPPLY_TYPE_USB). DCP/CDP are configured by the
# PMIC itself. Idempotent and no effect if it is already online.

US=/sys/class/power_supply/usb
TARGET=500000

[ -e "$US/input_current_limit" ] || exit 0

present=$(cat "$US/present" 2>/dev/null)
online=$(cat "$US/online" 2>/dev/null)
icl=$(cat "$US/input_current_limit" 2>/dev/null)

[ "$present" = "1" ] || exit 0
[ "$online" = "1" ] && exit 0

case "$icl" in ''|*[!0-9]*) exit 0 ;; esac
[ "$icl" -ge "$TARGET" ] && exit 0

echo "$TARGET" > "$US/input_current_limit" 2>/dev/null