FILESEXTRAPATHS:prepend:koi := "${THISDIR}/${PN}:"
COMPATIBLE_MACHINE:koi = "koi"

SRC_URI:append:koi = " file://init.machine.sh"

do_install:append:koi() {
    install -m 0755 ${UNPACKDIR}/init.machine.sh ${D}/init.machine
}
FILES:${PN}:append:koi = " /init.machine"

# The bootloader doesn't always properly recreate the FS on the userdata partition. Include e2fsprogs so the user can fix it if they need to.
RDEPENDS:${PN}:append:koi = " e2fsprogs"
