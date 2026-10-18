#!/bin/sh
# The ramoops region (console, panic records, Android pmsg) survives a warm
# reset and shows up in /sys/fs/pstore at the next boot, where systemd-pstore
# archives it under /var/lib/systemd/pstore. A plain reboot on aurora is a
# PMIC hard reset which loses DRAM, and the kernel sets the mode back to cold
# at every boot, so ask for a warm reset (PSCI SYSTEM_RESET2) here.
[ -w /sys/kernel/reboot/mode ] && echo warm > /sys/kernel/reboot/mode

# With the download-mode cookie armed (qcom_dload_mode's default) a panic or
# watchdog bite parks the watch in USB ramdump mode until the crown is held;
# unarmed, it reboots on its own and pstore keeps the panic record.
[ -w /sys/module/qcom_dload_mode/parameters/download_mode ] && \
	echo 0 > /sys/module/qcom_dload_mode/parameters/download_mode

# The ramoops console is the only kernel log that outlives a crash; at the
# bootloader's loglevel=6 it misses every info-level line (tty0 is a dummy).
[ -w /proc/sys/kernel/printk ] && echo 8 > /proc/sys/kernel/printk
exit 0
