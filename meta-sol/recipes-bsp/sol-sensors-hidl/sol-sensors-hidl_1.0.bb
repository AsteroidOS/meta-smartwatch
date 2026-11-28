SUMMARY = "sol: HIDL sensors HAL front-end so sensorfw can see the sensors"
DESCRIPTION = "sol's A14 vendor ships only the AIDL sensors service, while sensorfw's \
hybris plugin is a HIDL client looking up android.hardware.sensors@2.x::ISensors \
on hwbinder. With nothing answering, sensorfw spun in a retry loop and mce's \
sensor plugin loads failed with D-Bus NoReply. \
\
This ships android.hardware.sensors@2.1-service.multihal built from AOSP in a \
Halium 14 tree, plus the VINTF fragment A14's hwservicemanager needs before it \
accepts the registration. sol's USF sub-HAL (/vendor/lib/hw/sensors.usf.so) \
already exports sensorsHalGetSubHal_2_1, so only the front-end changes. \
\
A bridge: if sensorfw gains an AIDL sensors client, this recipe goes."

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

COMPATIBLE_MACHINE = "sol"
PACKAGE_ARCH = "${MACHINE_ARCH}"

# Prebuilt 32-bit ARM Android executable: our toolchain QA does not apply.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"
EXCLUDE_FROM_SHLIBS = "1"
# libdir: the payload sits under ${datadir} to be bind-mounted into the container.
INSANE_SKIP:${PN} = "already-stripped arch ldflags dev-so file-rdeps textrel staticdev libdir"

# sol-sensors-hidl-1.0.tar.gz carries the multihal executable. The VINTF
# fragments are our own XML.
SRC_URI = "https://mowerk.net/asteroidos/sol/sol-sensors-hidl-1.0.tar.gz \
           file://android.hardware.sensors@2.1-multihal.xml \
           file://sol-sensors-hidl.sh"
SRC_URI[sha256sum] = "d2aa498f93db27cba808437f685a6fad21545b2c4633267b0af9c773d2409b89"
SRC_URI[md5sum] = "a04166ce79bbc304a24cccaeb9d18919"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${datadir}/sol-sensors-hidl
    install -m 0755 "${UNPACKDIR}/android.hardware.sensors@2.1-service.multihal" \
        "${D}${datadir}/sol-sensors-hidl/android.hardware.sensors@2.1-service.multihal"
    install -m 0644 "${UNPACKDIR}/android.hardware.sensors@2.1-multihal.xml" \
        "${D}${datadir}/sol-sensors-hidl/android.hardware.sensors@2.1-multihal.xml"
    install -m 0644 ${UNPACKDIR}/README.txt ${D}${datadir}/sol-sensors-hidl/README.txt

    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/sol-sensors-hidl.sh ${D}${libexecdir}/sol-sensors-hidl.sh
}

# Nothing on the host executes these; they are bind-mounted into the container.
FILES:${PN} = "${datadir}/sol-sensors-hidl \
               ${libexecdir}/sol-sensors-hidl.sh"

RDEPENDS:${PN} = "sol-vendor-mount sol-lxc-android"

# No systemd unit: sol-lxc-android-start.sh runs this after setup_selinux and
# before lxc-start. It mounts a tmpfs with -o context=, which needs a loaded
# policy, and must be in place before Android init execs the sensors service.
# The start script guards on [ -x ], so install order does not matter.
