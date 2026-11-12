# Aurora has an NFC controller (NXP SN220U) reachable through the Android
# NFC HAL; nfcd's binder plugin drives it. Start nfcd by default; its mce
# plugin keeps the controller off while the display is off.
SYSTEMD_AUTO_ENABLE:${PN}:aurora = "enable"
