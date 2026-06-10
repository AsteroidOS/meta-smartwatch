# usb-moded triggers android-tools-adbd via StartUnit() after its
# configfs_set_function() has created the FunctionFS configfs dir;
# auto-starting at basic.target instead races configfs setup and leaves adbd
# holding the abstract socket "local:5037" with no /dev/usb-ffs/adb.
SYSTEMD_AUTO_ENABLE:${PN}-adbd:aurora = "disable"
