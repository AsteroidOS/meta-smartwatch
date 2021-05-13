FILESEXTRAPATHS_prepend_tetra := "${THISDIR}/mce:"
SRC_URI_append_tetra = " file://20als-defaults.ini \
        file://0001-fbdev-Don-t-use-ioctl-for-FBIOBLANK.patch"


do_install_append_tetra() {
    cp ../20als-defaults.ini ${D}/etc/mce/20als-defaults.ini
}
