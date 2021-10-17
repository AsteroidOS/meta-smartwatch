FILESEXTRAPATHS:prepend:tetra := "${THISDIR}/mce:"
SRC_URI:append:tetra = " file://20als-defaults.ini \
        file://0001-fbdev-Don-t-use-ioctl-for-FBIOBLANK.patch"


do_install:append:tetra() {
    cp ../20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
