FILESEXTRAPATHS:prepend:medaka := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:medaka = "medaka"

SRC_URI:append:medaka = " file://init.machine.sh \
    file://ld.config.28.txt"

do_install:append:medaka() {
    install -m 0755 ${UNPACKDIR}/init.machine.sh ${D}/init.machine
    install -m 0755 ${UNPACKDIR}/ld.config.28.txt ${D}/ld.config.28.txt
}
FILES:${PN}:append:medaka = " /ld.config.28.txt  /init.machine"

# 32bit busybox interacts with the 64bit kernel in a weird way that results in loops failing to mount. Hence include util-linux-mount in the ramdisk, as that seems to work fine.
# The bootloader doesn't always properly recreate the FS on the userdata partition. Include e2fsprogs so the user can fix it if they need to.
RDEPENDS:${PN}:append:medaka = " util-linux-mount e2fsprogs"
