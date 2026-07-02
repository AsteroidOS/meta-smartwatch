FILESEXTRAPATHS:prepend:aurora := "${THISDIR}/usb-moded:"

# Aurora has no /sys/class/android_usb interface -- only the configfs gadget
# framework. usb-moded's configfs branch handles this, but a few pieces have
# to be aurora-specific:
#
# 1. init_gfs stages the /config/usb_gadget/g1 skeleton at sysinit.target
#    because usb-moded's configfs_probe() bails out if g1 doesn't exist.  The
#    skeleton is just idVendor / idProduct / strings / configs/b.1 -- NOT the
#    ffs.adb function or its symlink. usb-moded is the sole writer of functions
#    and configs/b.1/<links>: trying to coexist with it leads to a race we
#    can't win (configfs_init zeroes UDC, configfs_set_function wipes
#    configs/b.1).
#
# 2. The stock usb-moded.service is used as-is: its init_ffs ExecStartPre is
#    `-`-tolerated and aurora doesn't ship init_ffs, and cable state comes
#    from the kernel extcon path (google-extcon-usb-shim), so no -f
#    cable-state pinning is needed.
#
# 3. Default mode is adb_mode (vs upstream's developer_mode = SSH/rndis)
#    because adb is aurora's primary debug channel. The default is shipped as
#    /etc/usb-moded/aurora-defaults.ini -- NOT as /etc/usb-moded/usb-moded.ini
#    -- because the latter is treated as a LEGACY config file by usb-moded: on
#    first successful migration to /var/lib/usb-moded/usb-moded.ini, it calls
#    config_remove_legacy_config() which `unlink()`s
#    /etc/usb-moded/usb-moded.ini permanently (usb_moded-config.c:988).
#    AsteroidOS has /var/lib on a volatile-bind (tmpfs) so the dynamic config
#    is wiped on every reboot, leaving NO default mode source from boot 2
#    onwards. The static-config glob at /etc/usb-moded/*.ini deliberately skips
#    USB_MODED_STATIC_CONFIG_FILE (= usb-moded.ini), so a different basename is
#    loaded each boot and never deleted.
#
# 4. dyn-modes/adb_mode.ini sets sysfs_value = ffs.adb. usb-moded's configfs
#    branch passes that value through configfs_register_function which mkdirs
#    /config/usb_gadget/g1/functions/<value> -- on configfs the function name
#    MUST have a "." separator (kernel drivers/usb/gadget/configfs.c
#    function_make()) so plain "adb" is rejected with -EINVAL. ffs.adb is the
#    configfs-correct name that pairs with the FunctionFS instance "adb"
#    mounted by adbd-prepare.service's adbd-functionfs.sh.
#
# 5. run/adb-startserver.ini drops `post = 1` so adbd runs in the appsync PRE
#    phase, and the modesetting patch in (6) does the actual ready wait. PRE
#    alone is not sufficient because StartUnit() returns when the job is
#    queued, not when adbd has reached ep0 + descriptor write.
#
# 6. modesetting patch: insert a poll for /dev/usb-ffs/adb/ep1 BETWEEN
#    configfs_set_function() and configfs_set_udc(). ep1 only exists when adbd
#    has FunctionFS in FFS_ACTIVE state (descriptors written), which is the
#    precondition the kernel composite_bind needs for ffs_func_bind() to
#    succeed. Without the wait, set_udc returns ENODEV and usb-moded falls back
#    to charging_only.
#
# 7. adbd-prepare.service is shipped as a STATIC unit (no [Install]) and pulled
#    in only via Wants=adbd-prepare.service on android-tools-adbd.service (set
#    in our 99-aurora-no-configfs.conf drop-in to android-tools). It
#    pre-creates /config/usb_gadget/g1/functions/ffs.adb in ExecStartPre so the
#    `mount -t functionfs adb` in adbd-functionfs.sh succeeds even if usb-moded
#    hasn't yet called configfs_set_function (race during
#    appsync_activate_pre's async StartUnit).
#
# 8. 10-aurora-android-tools.preset disables the upstream auto-enable of
#    android-tools-adbd.service so it only starts via usb-moded's
#    appsync. Auto-start at basic.target races our adbd-prepare with
#    usb-moded's configfs_set_function and ends up with adbd holding the
#    abstract socket "local:5037" without a valid /dev/usb-ffs/adb/ep0.
SRC_URI:append:aurora = " file://usb_moded.service \
                          file://init_gfs \
                          file://init_gfs.service \
                          file://aurora-defaults.ini \
                          file://dyn-modes/adb_mode.ini \
                          file://run/adb-startserver.ini \
                          file://adbd-prepare.service \
                          file://10-aurora-android-tools.preset \
                          file://0001-Wait-for-FunctionFS-FFS_ACTIVE-before-configfs_set_u.patch"

do_install:append:aurora() {
    install -m 0755 ${UNPACKDIR}/init_gfs ${D}/usr/bin/init_gfs
    install -m 0644 ${UNPACKDIR}/init_gfs.service \
        ${D}${systemd_unitdir}/system/init_gfs.service
    install -d ${D}${systemd_unitdir}/system/sysinit.target.wants
    ln -sf ../init_gfs.service \
        ${D}${systemd_unitdir}/system/sysinit.target.wants/init_gfs.service
    # adbd-prepare.service is shipped as a static unit (no [Install] section)
    # and is only pulled in via Wants= on android-tools-adbd (see
    # meta-aurora/recipes-devtools/android-tools/files/
    # 99-aurora-no-configfs.conf). Do NOT add a basic.target.wants symlink here
    # -- auto-starting it would race usb-moded's configfs_set_function() and
    # break adb (see comment block above, point 7).
    install -m 0644 ${UNPACKDIR}/adbd-prepare.service \
        ${D}${systemd_unitdir}/system/adbd-prepare.service
    install -d ${D}${systemd_unitdir}/system-preset
    install -m 0644 ${UNPACKDIR}/10-aurora-android-tools.preset \
        ${D}${systemd_unitdir}/system-preset/10-aurora-android-tools.preset
    # /etc/usb-moded/aurora-defaults.ini: matched by the static-config glob
    # /etc/usb-moded/*.ini and NOT the legacy delete-on-first-boot path (which
    # only targets /etc/usb-moded/usb-moded.ini).
    install -m 0644 ${UNPACKDIR}/aurora-defaults.ini \
        ${D}/etc/usb-moded/aurora-defaults.ini
    # Delete the legacy /etc/usb-moded/usb-moded.ini that meta-asteroid ships
    # (mode=developer_mode). On first boot usb-moded would load it via
    # config_load_legacy_config, migrate `mode=developer_mode` into
    # /var/lib/usb-moded/usb-moded.ini (overlayfs upper tmpfs), then unlink the
    # legacy file. config_get_mode_setting reads static + dynamic (NOT legacy),
    # so dynamic = developer_mode would override our aurora-defaults.ini static
    # = adb_mode -> selected mode would be developer_mode (and on the next
    # boot, after tmpfs wipes dynamic, the legacy file is already gone for
    # good, leaving only static = adb_mode -- so this only mis-fires for boot
    # 1, but that's enough to break the very first usable boot after flashing).
    rm -f ${D}/etc/usb-moded/usb-moded.ini
    install -m 0644 ${UNPACKDIR}/dyn-modes/adb_mode.ini \
        ${D}/etc/usb-moded/dyn-modes/adb_mode.ini
    install -m 0644 ${UNPACKDIR}/run/adb-startserver.ini \
        ${D}/etc/usb-moded/run/adb-startserver.ini
}

FILES:${PN}:append:aurora = " ${systemd_unitdir}/system/init_gfs.service \
                              ${systemd_unitdir}/system/sysinit.target.wants/init_gfs.service \
                              ${systemd_unitdir}/system/adbd-prepare.service \
                              ${systemd_unitdir}/system-preset/10-aurora-android-tools.preset"
