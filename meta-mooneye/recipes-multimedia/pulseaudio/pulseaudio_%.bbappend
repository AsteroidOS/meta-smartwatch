# Don't offer Bluetooth audio (A2DP) on mooneye. Phones then treat the watch
# as headphones and stream audio to it, which overflows the MT6630's receive
# buffer: the firmware asserts (RB_FULL) and the whole chip resets, again on
# every reconnect. module-bluetooth-discover loads module-bluez5-discover by
# itself, so disable both, and the policy module that only serves them.
do_install:append:mooneye() {
    for m in module-bluetooth-policy module-bluetooth-discover module-bluez5-discover; do
        if ! grep -q "^load-module $m\$" ${D}${sysconfdir}/pulse/default.pa; then
            bbfatal "default.pa: no 'load-module $m' line; mooneye quirk not applied"
        fi
        sed -i "s/^load-module $m\$/#&/" ${D}${sysconfdir}/pulse/default.pa
    done
}
