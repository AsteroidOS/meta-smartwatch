# /system/ directory build instructions [WIP]

This document will eventually describe how to build the system-dir tarball, needed for the narwhal platform to function properly.

## 1. Know how to boot to fastboot and recovery

You will need to know how to get into the fastboot menu. We have not had success with booting to recovery.

To get into the fastboot menu you can use the `adb reboot bootloader` command when booted to Android Wear. Make sure you have ADB debugging enabled on Android Wear.
From the fastboot menu you can boot to the Android Recovery.

You can also get to the fastboot menu by powering on the watch while holding the lower side pusher.

