FILESEXTRAPATHS:prepend:sol := "${THISDIR}/usb-moded:"

# Ported from meta-aurora; same SoC, same GKI 5.15 kernel, same failure: adb
# comes up `offline` because adbd holds the abstract socket "local:5037"
# without a valid /dev/usb-ffs/adb/ep0. sol has only the configfs gadget
# framework, no /sys/class/android_usb.
#
# 1. init_gfs stages the /config/usb_gadget/g1 skeleton at sysinit.target,
#    because configfs_probe() bails out if g1 does not exist. Only idVendor,
#    idProduct, strings and configs/b.1: usb-moded must be the sole writer of
#    functions and their links (configfs_init zeroes UDC, configfs_set_function
#    wipes configs/b.1). On sol it also removes the gadget the initramfs adbd
#    leaves bound across switch_root, on every boot.
#
# 2. usb-moded.service is the stock unit. init_ffs is not shipped (its
#    ExecStartPre is `-`-tolerated) and cable state comes from the kernel extcon
#    (google-extcon-shim), so no -f cable-state pinning.
#
# 3. Default mode is adb_mode, shipped as /etc/usb-moded/sol-defaults.ini.
#    usb-moded treats /etc/usb-moded/usb-moded.ini as legacy and unlinks it
#    after migrating it to /var/lib/usb-moded/ (usb_moded-config.c:988), and
#    /var/lib is tmpfs here, so from the second boot there would be no default
#    at all. The /etc/usb-moded/*.ini static glob skips usb-moded.ini, so a
#    different basename is read every boot and never deleted.
#
# 4. dyn-modes/adb_mode.ini uses sysfs_value = ffs.adb. configfs function names
#    need a "." (drivers/usb/gadget/configfs.c function_make()), so plain "adb"
#    gets -EINVAL. ffs.adb pairs with the FunctionFS instance "adb" that
#    adbd-functionfs.sh mounts.
#
# 5. run/adb-startserver.ini drops `post = 1` so adbd starts in the appsync PRE
#    phase. PRE alone is not enough, since StartUnit() returns when the job is
#    queued; the patch in (6) does the wait.
#
# 6. The modesetting patch polls for /dev/usb-ffs/adb/ep1 between
#    configfs_set_function() and configfs_set_udc(). ep1 exists only once adbd
#    has written its descriptors (FFS_ACTIVE), which ffs_func_bind() needs.
#    Without the wait set_udc returns ENODEV and usb-moded falls back to
#    charging_only.
#
# 7. adbd-prepare.service is a static unit pulled in by android-tools-adbd
#    (99-sol-no-configfs.conf); see the unit's header.
#
# 8. 10-sol-android-tools.preset keeps android-tools-adbd from auto-starting;
#    see the preset's header.
SRC_URI:append:sol = " file://usb-moded.service \
                       file://init_gfs \
                       file://init_gfs.service \
                       file://sol-defaults.ini \
                       file://dyn-modes/adb_mode.ini \
                       file://run/adb-startserver.ini \
                       file://adbd-prepare.service \
                       file://10-sol-android-tools.preset \
                       file://0001-Wait-for-FunctionFS-FFS_ACTIVE-before-configfs_set_u.patch"

do_install:append:sol() {
    install -m 0755 ${UNPACKDIR}/init_gfs ${D}/usr/bin/init_gfs
    install -m 0644 ${UNPACKDIR}/init_gfs.service \
        ${D}${systemd_unitdir}/system/init_gfs.service
    install -d ${D}${systemd_unitdir}/system/sysinit.target.wants
    ln -sf ../init_gfs.service \
        ${D}${systemd_unitdir}/system/sysinit.target.wants/init_gfs.service
    # Static unit: no basic.target.wants symlink, see point 7.
    install -m 0644 ${UNPACKDIR}/adbd-prepare.service \
        ${D}${systemd_unitdir}/system/adbd-prepare.service
    install -d ${D}${systemd_unitdir}/system-preset
    install -m 0644 ${UNPACKDIR}/10-sol-android-tools.preset \
        ${D}${systemd_unitdir}/system-preset/10-sol-android-tools.preset
    # sol-defaults.ini, not usb-moded.ini: see point 3.
    install -m 0644 ${UNPACKDIR}/sol-defaults.ini \
        ${D}/etc/usb-moded/sol-defaults.ini
    # Drop meta-asteroid's legacy usb-moded.ini (mode=developer_mode). On first
    # boot usb-moded would migrate it into the dynamic config, which overrides
    # sol-defaults.ini, so the first boot after a flash would come up without adb.
    rm -f ${D}/etc/usb-moded/usb-moded.ini
    install -m 0644 ${UNPACKDIR}/dyn-modes/adb_mode.ini \
        ${D}/etc/usb-moded/dyn-modes/adb_mode.ini
    install -m 0644 ${UNPACKDIR}/run/adb-startserver.ini \
        ${D}/etc/usb-moded/run/adb-startserver.ini
}

FILES:${PN}:append:sol = " ${systemd_unitdir}/system/init_gfs.service \
                           ${systemd_unitdir}/system/sysinit.target.wants/init_gfs.service \
                           ${systemd_unitdir}/system/adbd-prepare.service \
                           ${systemd_unitdir}/system-preset/10-sol-android-tools.preset"
