SUMMARY = "sol: libhybris system image and the LXC container setup for the vendor HALs"
DESCRIPTION = "Ships the Halium A13 system image libhybris loads, the systemd glue that \
loop-mounts the container's system image and binds its flattened APEXes over \
/apex, and the LXC container setup that runs sol's vendor HALs (composer, \
sensors, audio, Bluetooth), plus the container's Android 14 system image. \
\
sol's own stock Android 14 /system cannot be used under libhybris: libhybris \
skips libc.so's constructors and stock A14 bionic then dereferences NULL before \
main(). A Halium-built system is made to be loaded this way, so sol runs its \
own kernel and A14 vendor with a Halium A13 system."

# GPL-3.0-only: the scripts and units. Proprietary: the container image, which
# is sol's stock Android 14 system (Google's).
LICENSE = "GPL-3.0-only & Proprietary"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891 \
                    file://${COMMON_LICENSE_DIR}/Proprietary;md5=0557f9d92cf58f2ccdd50f62f8ac0b28"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
INSANE_SKIP:${PN} = "already-stripped arch ldflags dev-so file-rdeps"

inherit systemd

# INTERIM: reuses meta-aurora's Halium-13 bundle verbatim (same monaco SoC),
# fetched from a byte-identical mirror of meta-aurora's Dropbox download (same
# md5). sol should get its own Halium system built against its A14 vendor.
SRC_URI = "https://mowerk.net/asteroidos/sol/aurora-lxc-android-bundle.tar.gz \
           file://sol-android-rootfs.mount \
           file://sol-hybris-system.mount \
           file://sol-android-apex.service \
           file://sol-android-apex.sh \
           file://sol-lxc-android-start.sh \
           file://sol-lxc-android.service \
           file://mount-binder.sh \
           file://bluebinder-sol.conf \
           https://mowerk.net/asteroidos/sol/sol-halium-a14-1.0.img.zst;name=container;unpack=0"
SRC_URI[md5sum] = "145a40e6e863afa6a00be6ad0fa28ea4"
SRC_URI[sha256sum] = "c0de79821720452d31f1fea313aa74566b6791158eb399de98428744409a16c5"
SRC_URI[container.sha256sum] = "8029777c0a85e7b810f0290fc1970436344a8ba14bcce902f68b6972113bc4b8"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "sol-vendor-mount lxc zstd"
PACKAGE_WRITE_DEPS += "zstd-native"

do_install() {
    install -d ${D}${localstatedir}/lib/lxc/android/rootfs
    # Two systems. aurora's Halium A13 image is the host's libhybris system
    # (stock A14 bionic crashes under libhybris). The container needs an A14
    # system, because A13 init cannot parse sol's A14 vendor files: that one is
    # sol's stock Android 14 system with APEXes flattened, SELinux labels
    # applied and two Halium shims added.
    install -m 0644 ${UNPACKDIR}/android-rootfs.img ${D}${localstatedir}/lib/lxc/android/hybris-system.img
    # The container image is 2.2 GB unpacked. opkg on 32-bit ARM stores a
    # package's installed size in an unsigned long and prints it signed, so a
    # package over 2 GiB gets a negative Installed-Size that it then fails to
    # parse. Ship it compressed; pkg_postinst unpacks it.
    install -m 0644 ${UNPACKDIR}/sol-halium-a14-1.0.img.zst ${D}${localstatedir}/lib/lxc/android/android-rootfs.img.zst

    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-android-apex.sh ${D}${libexecdir}/sol-android-apex.sh

    # LXC bundle: config plus the container's own hooks.
    install -m 0644 ${UNPACKDIR}/config       ${D}${localstatedir}/lib/lxc/android/
    install -m 0755 ${UNPACKDIR}/mount.sh     ${D}${localstatedir}/lib/lxc/android/
    install -m 0755 ${UNPACKDIR}/pre-start.sh ${D}${localstatedir}/lib/lxc/android/
    install -m 0755 ${UNPACKDIR}/post-stop.sh ${D}${localstatedir}/lib/lxc/android/
    install -m 0755 ${UNPACKDIR}/mount-binder.sh ${D}${localstatedir}/lib/lxc/android/

    # meta-virtualization's liblxc is built without AppArmor.
    sed -i '/^lxc\.apparmor\./d' ${D}${localstatedir}/lib/lxc/android/config

    # Share the host's IPC namespace. Binder state is per-IPC-namespace, so with
    # a separate one the host's /dev/hwbinder is an empty context and libhybris
    # in the launcher cannot reach the container's composer.
    sed -i 's/^lxc\.namespace\.keep[[:space:]]*=.*$/lxc.namespace.keep = net user ipc/' \
        ${D}${localstatedir}/lib/lxc/android/config

    # Android init reboots the host if a critical service crash-loops.
    echo 'lxc.cap.drop = sys_boot' >> ${D}${localstatedir}/lib/lxc/android/config
    # CLOCK_REALTIME is not namespaced; without this the container sets the
    # host clock from a volatile RTC.
    echo 'lxc.cap.drop = sys_time' >> ${D}${localstatedir}/lib/lxc/android/config

    echo 'lxc.console.logfile = /var/log/lxc-android-console.log' >> ${D}${localstatedir}/lib/lxc/android/config
    echo 'lxc.console.buffer.size = 1MB' >> ${D}${localstatedir}/lib/lxc/android/config
    echo 'lxc.hook.mount = /var/lib/lxc/android/mount-binder.sh' >> ${D}${localstatedir}/lib/lxc/android/config

    install -m 0755 ${UNPACKDIR}/sol-lxc-android-start.sh ${D}${libexecdir}/sol-lxc-android-start.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/sol-android-rootfs.mount \
        ${D}${systemd_unitdir}/system/var-lib-lxc-android-rootfs.mount
    install -m 0644 ${UNPACKDIR}/sol-hybris-system.mount \
        ${D}${systemd_unitdir}/system/android-hybris.mount
    install -m 0644 ${UNPACKDIR}/sol-android-apex.service \
        ${D}${systemd_unitdir}/system/sol-android-apex.service
    install -m 0644 ${UNPACKDIR}/sol-lxc-android.service \
        ${D}${systemd_unitdir}/system/sol-lxc-android.service

    # bluebinder ordering and HAL wait: see the comment in bluebinder-sol.conf.
    install -d ${D}${systemd_unitdir}/system/bluebinder.service.d
    install -m 0644 ${UNPACKDIR}/bluebinder-sol.conf \
        ${D}${systemd_unitdir}/system/bluebinder.service.d/bluebinder-sol.conf
}

# Runs at rootfs time ($D set) and on the watch. Unpack beside the image and
# rename over it: the old one may be loop-mounted by a running container.
pkg_postinst:${PN}() {
    d=$D${localstatedir}/lib/lxc/android
    if [ -f $d/android-rootfs.img.zst ]; then
        zstd -d -q -f $d/android-rootfs.img.zst -o $d/android-rootfs.img.new &&
            mv -f $d/android-rootfs.img.new $d/android-rootfs.img &&
            rm -f $d/android-rootfs.img.zst || { rm -f $d/android-rootfs.img.new; exit 1; }
    fi
}

pkg_postrm:${PN}() {
    case "$1" in
        remove|purge) rm -f $D${localstatedir}/lib/lxc/android/android-rootfs.img ;;
    esac
}

# Shipped Android binaries; skip the QA that assumes our own toolchain.
do_package_qa() {
}

# If the container ever wedges boot, `systemctl disable sol-lxc-android` from
# adb recovers: adb comes up independently of the container.
SYSTEMD_SERVICE:${PN} = "android-hybris.mount var-lib-lxc-android-rootfs.mount sol-android-apex.service sol-lxc-android.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
FILES:${PN} = "${localstatedir}/lib/lxc/android \
               ${libexecdir}/sol-android-apex.sh \
               ${libexecdir}/sol-lxc-android-start.sh \
               ${systemd_unitdir}/system/sol-lxc-android.service \
               ${systemd_unitdir}/system/var-lib-lxc-android-rootfs.mount \
               ${systemd_unitdir}/system/android-hybris.mount \
               ${systemd_unitdir}/system/sol-android-apex.service \
               ${systemd_unitdir}/system/bluebinder.service.d/bluebinder-sol.conf"
EXCLUDE_FROM_SHLIBS = "1"
