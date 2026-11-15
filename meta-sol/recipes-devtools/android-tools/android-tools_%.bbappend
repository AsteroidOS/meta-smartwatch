# From meta-aurora. usb-moded starts adbd via StartUnit() once the FunctionFS
# configfs dir exists. Auto-starting at basic.target races that setup: adbd holds
# "local:5037" with no /dev/usb-ffs/adb, shows as `offline`, and each
# Restart=on-failure re-runs android-gadget-setup, so the gadget flaps.
SYSTEMD_AUTO_ENABLE:${PN}-adbd:sol = "disable"
