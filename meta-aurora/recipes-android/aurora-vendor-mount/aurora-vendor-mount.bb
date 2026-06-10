SUMMARY = "Recreate the dm-linear mapping for aurora's /vendor partition and mount it"
DESCRIPTION = "On stock Android-13/Halium-13, aurora's vendor partition is a \
logical partition packed inside /super (/dev/mmcblk0p80 on slot B). Android \
first-stage init parses LP metadata and creates /dev/mapper/vendor_b via dm-linear; \
our shell-script initramfs doesn't, and the Android-9 init we ship at \
/usr/libexec/hal-droid/system/bin/init pre-dates dynamic partitions and \
can't either. Workaround: hardcode the dm table observed from a working \
UBPorts install (dmsetup table | grep vendor_b), recreate it at boot \
before android-init runs. Avoids duplicating ~234 MB of vendor data into \
our rootfs.img."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "aurora"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit systemd

SRC_URI = "file://aurora-vendor-mount.sh \
           file://aurora-vendor-mount.service"
S = "${UNPACKDIR}"

RDEPENDS:${PN} = "lvm2 util-linux-mount"

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/aurora-vendor-mount.sh ${D}${libexecdir}/aurora-vendor-mount.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/aurora-vendor-mount.service \
        ${D}${systemd_unitdir}/system/aurora-vendor-mount.service

    install -d ${D}${sysconfdir}/systemd/system/local-fs.target.wants
    ln -sf ../../../systemd/system/aurora-vendor-mount.service \
        ${D}${sysconfdir}/systemd/system/local-fs.target.wants/aurora-vendor-mount.service

    install -d ${D}/vendor
}

SYSTEMD_SERVICE:${PN} = "aurora-vendor-mount.service"
FILES:${PN} = "${libexecdir}/aurora-vendor-mount.sh \
               ${systemd_unitdir}/system/aurora-vendor-mount.service \
               ${sysconfdir}/systemd/system/local-fs.target.wants/aurora-vendor-mount.service \
               /vendor"
