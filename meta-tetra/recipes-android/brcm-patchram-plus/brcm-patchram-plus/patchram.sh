#!/bin/bash

echo $entrytime " applying rampatch to BCM43341B0 bluetooth adapter"

# wait for hci0 to become available and usable
sleep 1
# In case bluetooth is softblocked (before patch)
RFKILLSOFT=$(rfkill list | grep Bluetooth -n2 | grep Soft | grep yes)
if [ "$RFKILLSOFT" ]; then 
	echo "Bluetooth was soft disabled before applying patch"
	rfkill unblock bluetooth
	hciconfig hci0 reset
fi
brcm_patchram_plus -d --enable_hci --no2bytes --patchram /vendor/firmware/BCM43341B0_002.001.014.0122.0181.hcd /dev/ttyHS99

# In case something happened and its blocked after uploading the patch
RFKILLSOFT=$(rfkill list | grep Bluetooth -n2 | grep Soft | grep yes)
if [ "$RFKILLSOFT" ]; then 
	echo "Bluetooth was soft disabled (after patch)"
	rfkill unblock bluetooth
	hciconfig hci0 reset
fi
