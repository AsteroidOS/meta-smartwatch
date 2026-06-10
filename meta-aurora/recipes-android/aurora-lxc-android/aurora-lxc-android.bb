SUMMARY = "Aurora: Halium-13 LXC container (bundle + runner)"
DESCRIPTION = "Ships /var/lib/lxc/android/ -- the LXC config + Halium-13 \
android rootfs.img + container hooks -- and the systemd glue that starts \
the container at boot. Inside the container, Google's stock Android-13 \
init brings up hwcomposer, servicemanager, and the vendor HALs that \
libhybris bridges to Qt/lipstick on the host."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "aurora"
PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
INSANE_SKIP:${PN} = "already-stripped arch ldflags dev-so file-rdeps"

inherit systemd

# aurora-lxc-android-bundle.tar.gz holds: android-rootfs.img + config +
# mount.sh + pre-start.sh + post-stop.sh + pre-start.d/* + overrides/* as
# Extracted from /var/lib/lxc/android/ on a running UBPorts install.
SRC_URI = "https://dl.dropboxusercontent.com/scl/fi/insvp7s5wp516iv56a4vx/aurora-lxc-android.tar.gz?rlkey=xp4m8ayfwyn12nqae87o3sm3t&dl=1;downloadfilename=aurora-lxc-android-bundle.tar.gz \
           file://mount-binder.sh \
           file://aurora-lxc-android.service \
           file://aurora-lxc-android-rootfs.mount \
           file://aurora-lxc-android-start.sh \
           file://aurora-no-kmsg.conf \
           file://bluebinder-aurora.conf"
SRC_URI[md5sum] = "145a40e6e863afa6a00be6ad0fa28ea4"
S = "${UNPACKDIR}"

RDEPENDS:${PN} = "lxc aurora-vendor-mount"

do_install() {
    install -d ${D}${localstatedir}/lib/lxc/android \
               ${D}${localstatedir}/lib/lxc/android/pre-start.d \
               ${D}${localstatedir}/lib/lxc/android/overrides \
               ${D}${localstatedir}/lib/lxc/android/rootfs

    install -m 0644 ${UNPACKDIR}/android-rootfs.img ${D}${localstatedir}/lib/lxc/android/
    install -m 0644 ${UNPACKDIR}/config             ${D}${localstatedir}/lib/lxc/android/
    install -m 0755 ${UNPACKDIR}/mount.sh           ${D}${localstatedir}/lib/lxc/android/
    install -m 0755 ${UNPACKDIR}/pre-start.sh       ${D}${localstatedir}/lib/lxc/android/
    install -m 0755 ${UNPACKDIR}/post-stop.sh       ${D}${localstatedir}/lib/lxc/android/
    install -m 0755 ${UNPACKDIR}/mount-binder.sh    ${D}${localstatedir}/lib/lxc/android/

    # UBPorts' config sets `lxc.apparmor.profile = unconfined` but
    # meta-virtualization's liblxc is built without AppArmor, we don't want it.
    sed -i '/^lxc\.apparmor\./d' ${D}${localstatedir}/lib/lxc/android/config

    # Share the host's IPC namespace with the container.
    #
    # Daemons on the host (sensorfwd's HybrisManager, ngfd-droid-vibrator,
    # bluebinder, etc.) reach Android HALs by talking to hwservicemanager over
    # /dev/hwbinder. The binder driver isolates state per IPC namespace -- with
    # the default LXC setup, the container creates a separate IPC ns, so host's
    # /dev/hwbinder is an empty binder context.
    sed -i 's/^lxc\.namespace\.keep[[:space:]]*=.*$/lxc.namespace.keep = net user ipc/' \
        ${D}${localstatedir}/lib/lxc/android/config

    # Drop CAP_SYS_BOOT inside the container. Android's init responds to a
    # crash-looping critical service (servicemanager, etc.) by calling reboot()
    # -- with CAP_SYS_BOOT retained, that reboots the host.
    echo 'lxc.cap.drop = sys_boot' >> ${D}${localstatedir}/lib/lxc/android/config

    # Drop CAP_SYS_TIME inside the container. CLOCK_REALTIME is NOT namespaced,
    # so the container shares the host's wall clock. At boot Android's userspace
    # (system_server / AlarmManager / a time HAL) sets the system clock from the
    # pm8xxx RTC, which on aurora is VOLATILE and reads ~1970 -- stomping the
    # correct time that swclock-offset restored at boot (and that DSME, with the
    # read-only-RTC kernel patch, deliberately refuses to source from the RTC).
    # This was the "date resets to 1970 on every reboot" bug: not DSME, timed,
    # timedated nor the kernel, but the container settimeofday()'ing the shared
    # clock to the raw RTC right after it starts. Without CAP_SYS_TIME the
    # container's settimeofday/clock_settime return EPERM, so it can no longer
    # move the host clock; Android tolerates the failure and the host time set by
    # swclock-offset / the phone (timedated) survives.
    echo 'lxc.cap.drop = sys_time' >> ${D}${localstatedir}/lib/lxc/android/config

    # Capture container stdout/stderr so debugging is possible. Android init
    # redirects its own stdio to /dev/null and logs via /dev/kmsg, but the
    # bionic linker writes early warnings to stderr before that redirect runs.
    echo 'lxc.console.logfile = /var/log/lxc-android-console.log' >> ${D}${localstatedir}/lib/lxc/android/config
    echo 'lxc.console.buffer.size = 1MB' >> ${D}${localstatedir}/lib/lxc/android/config

    # Second mount hook: create /dev/{binder,hwbinder,vndbinder} inside the
    # container. Without these, servicemanager dies "cannot open /dev/binder"
    # and Android init crash-loops it. lxc.hook.mount is cumulative; this runs
    # after the tarball's mount.sh.
    echo 'lxc.hook.mount = /var/lib/lxc/android/mount-binder.sh' >> ${D}${localstatedir}/lib/lxc/android/config

    # Optional pre-start.d/ and overrides/ from the bundle.
    if [ -n "$(ls -A ${UNPACKDIR}/pre-start.d 2>/dev/null)" ]; then
        cp -a ${UNPACKDIR}/pre-start.d/. ${D}${localstatedir}/lib/lxc/android/pre-start.d/
        chmod 0755 ${D}${localstatedir}/lib/lxc/android/pre-start.d/*
    fi
    if [ -n "$(ls -A ${UNPACKDIR}/overrides 2>/dev/null)" ]; then
        cp -a ${UNPACKDIR}/overrides/. ${D}${localstatedir}/lib/lxc/android/overrides/
    fi

    # systemd unit + start script
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/aurora-lxc-android-start.sh \
        ${D}${libexecdir}/aurora-lxc-android-start.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/aurora-lxc-android.service \
        ${D}${systemd_unitdir}/system/aurora-lxc-android.service
    install -m 0644 ${UNPACKDIR}/aurora-lxc-android-rootfs.mount \
        ${D}${systemd_unitdir}/system/var-lib-lxc-android-rootfs.mount

    # bluebinder ordering drop-in: see the comment in bluebinder-aurora.conf.
    install -d ${D}${systemd_unitdir}/system/bluebinder.service.d
    install -m 0644 ${UNPACKDIR}/bluebinder-aurora.conf \
        ${D}${systemd_unitdir}/system/bluebinder.service.d/bluebinder-aurora.conf

    # journald drop-in: keep journald from ingesting kmsg. Vendor kernel
    # drivers (eg: sw5100_bms) storm kmsg when no Android daemon is feeding
    # them, which floods journald and stalls systemd.
    install -d ${D}${sysconfdir}/systemd/journald.conf.d
    install -m 0644 ${UNPACKDIR}/aurora-no-kmsg.conf \
        ${D}${sysconfdir}/systemd/journald.conf.d/aurora-no-kmsg.conf
}

# Skip QA on shipped Android files.
do_package_qa() {
}

SYSTEMD_SERVICE:${PN} = "aurora-lxc-android.service var-lib-lxc-android-rootfs.mount"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
FILES:${PN} = "${localstatedir}/lib/lxc/android \
               ${libexecdir}/aurora-lxc-android-start.sh \
               ${systemd_unitdir}/system/aurora-lxc-android.service \
               ${systemd_unitdir}/system/var-lib-lxc-android-rootfs.mount \
               ${systemd_unitdir}/system/bluebinder.service.d/bluebinder-aurora.conf \
               ${sysconfdir}/systemd/journald.conf.d/aurora-no-kmsg.conf"
EXCLUDE_FROM_SHLIBS = "1"
