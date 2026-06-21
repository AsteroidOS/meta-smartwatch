inherit gettext

SUMMARY = "Downloads the LG Watch Urbane /system and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"
SRC_URI = "https://dl.dropboxusercontent.com/s/hta18rrkn7wenc9/system-M1D64S.tar.gz \
           file://fw_bcmdhd.bin_a1 \
           file://fw_bcmdhd_apsta.bin_a1 \
           file://bcmdhd.cal_a1 \
           file://adsp.mdt \
           file://adsp.b00 \
           file://adsp.b01 \
           file://adsp.b02 \
           file://adsp.b03 \
           file://adsp.b04 \
           file://adsp.b05 \
           file://adsp.b06 \
           file://adsp.b07 \
           file://adsp.b08 \
           file://adsp.b10 \
           file://adsp.b11 \
           file://sensor_def_nemo.conf \
           file://sensors.msm8226.so \
           "
SRC_URI[md5sum] = "a20e105e7d5f38c127e0e0c1f7808999"
SRC_URI[sha256sum] = "36327f17517bc1d850d2f5b4b774e39d514946413f58b387306fbf19f2684fc4"
PV = "marshmallow"

PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_PACKAGE_STRIP = "1"
COMPATIBLE_MACHINE = "nemo"
FILESEXTRAPATHS:prepend:nemo := "${THISDIR}/files:"
INSANE_SKIP:${PN} = "already-stripped"
S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"
B = "${S}"

PROVIDES += "virtual/android-system-image"
PROVIDES += "virtual/android-headers"

do_install() {
    install -d ${D}/system/
    cp -r system/* ${D}/system/

    install -d ${D}/usr/
    cp -r usr/* ${D}/usr/

    install -d ${D}${includedir}/android
    cp -r include/* ${D}${includedir}/android/

    install -d ${D}${libdir}/pkgconfig
    install -m 0644 ${D}${includedir}/android/android-headers.pc ${D}${libdir}/pkgconfig
    rm ${D}${includedir}/android/android-headers.pc
    cd ${D}
    ln -s system/vendor vendor

    mkdir etc/
    ln -s /system/etc/firmware etc/firmware
}

do_install:append() {
    install -m 0644 ${UNPACKDIR}/fw_bcmdhd.bin_a1 ${D}/system/vendor/firmware/fw_bcmdhd.bin_a1
    install -m 0644 ${UNPACKDIR}/fw_bcmdhd_apsta.bin_a1 ${D}/system/vendor/firmware/fw_bcmdhd_apsta.bin_a1
    install -d ${D}/system/etc/wifi/
    install -m 0644 ${UNPACKDIR}/bcmdhd.cal_a1 ${D}/system/etc/wifi/bcmdhd.cal_a1
    install -m 0644 ${UNPACKDIR}/adsp.mdt ${D}/system/vendor/firmware/adsp.mdt
    install -m 0644 ${UNPACKDIR}/adsp.b00 ${D}/system/vendor/firmware/adsp.b00
    install -m 0644 ${UNPACKDIR}/adsp.b01 ${D}/system/vendor/firmware/adsp.b01
    install -m 0644 ${UNPACKDIR}/adsp.b02 ${D}/system/vendor/firmware/adsp.b02
    install -m 0644 ${UNPACKDIR}/adsp.b03 ${D}/system/vendor/firmware/adsp.b03
    install -m 0644 ${UNPACKDIR}/adsp.b04 ${D}/system/vendor/firmware/adsp.b04
    install -m 0644 ${UNPACKDIR}/adsp.b05 ${D}/system/vendor/firmware/adsp.b05
    install -m 0644 ${UNPACKDIR}/adsp.b06 ${D}/system/vendor/firmware/adsp.b06
    install -m 0644 ${UNPACKDIR}/adsp.b07 ${D}/system/vendor/firmware/adsp.b07
    install -m 0644 ${UNPACKDIR}/adsp.b08 ${D}/system/vendor/firmware/adsp.b08
    install -m 0644 ${UNPACKDIR}/adsp.b10 ${D}/system/vendor/firmware/adsp.b10
    install -m 0644 ${UNPACKDIR}/adsp.b11 ${D}/system/vendor/firmware/adsp.b11
    install -d ${D}/system/etc/sensors/
    install -m 0644 ${UNPACKDIR}/sensor_def_nemo.conf ${D}/system/etc/sensors/sensor_def_nemo.conf
    install -m 0755 ${UNPACKDIR}/sensors.msm8226.so ${D}/system/vendor/lib/hw/sensors.msm8226.so
    sed -i \
        -e 's/^ro.product.name=bass$/ro.product.name=nemo/' \
        -e 's/^ro.product.device=bass$/ro.product.device=nemo/' \
        -e 's/^ro.product.board=bass$/ro.product.board=nemo/' \
        ${D}/system/build.prop
    ln -sf /usr/libexec/hal-droid/system/lib/hw/hwcomposer.msm8226.so ${D}/system/lib/hw/hwcomposer.nemo.so
    ln -sf /usr/libexec/hal-droid/system/lib/hw/gralloc.msm8226.so ${D}/system/lib/hw/gralloc.nemo.so
    ln -sf /usr/libexec/hal-droid/system/lib/hw/memtrack.msm8226.so ${D}/system/lib/hw/memtrack.nemo.so
    ln -sf lights.bass.so ${D}/system/lib/hw/lights.nemo.so
    ln -sf power.bass.so ${D}/system/lib/hw/power.nemo.so
    ln -sf sensors.msm8226.so ${D}/system/vendor/lib/hw/sensors.nemo.so
}

# FIXME: QA Issue: Architecture did not match (40 to 164) on /work/nemo-oe-linux-gnueabi/android/lollipop-r0/packages-split/android-system/system/vendor/firmware/adsp.b00 [arch]
do_package_qa() {
}

PACKAGES =+ "android-system android-headers"
FILES:android-system = "/system /vendor /etc /usr"
FILES:android-headers = "${libdir}/pkgconfig ${includedir}/android"
EXCLUDE_FROM_SHLIBS = "1"
