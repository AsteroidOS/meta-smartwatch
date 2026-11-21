# geoclue-hybris adds the gps group to its supplementary groups with
# setgroups() and aborts if that fails; upstream installs it setuid root for
# this. It runs as the user here, so give it only CAP_SETGID. setcap runs on
# the target when the package is installed there.
PACKAGE_WRITE_DEPS:append:mooneye = " libcap-native"
RDEPENDS:${PN}:append:mooneye = " libcap-bin"
pkg_postinst:${PN}:append:mooneye() {
    setcap cap_setgid+ep $D${libexecdir}/geoclue-hybris
}
