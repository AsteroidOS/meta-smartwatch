COMPATIBLE_MACHINE:aurora = "aurora"

# The android-pie64 tarball (msm8909w-based) installs the Halium init,
# servicemanager, logd, linker, and 100+ libhybris-compat .so's at
# /usr/libexec/hal-droid/system/{bin,lib,etc}. BUT the ELF binaries inside
# (e.g. servicemanager) have PT_INTERP = /system/bin/linker baked
# in -- that's the path Android binaries unconditionally encode. Without a
# /system tree, every dynamically-linked binary execve()d by init fails.
#
# Create /system as a symlink to /usr/libexec/hal-droid/system, so
# /system/bin/linker resolves to the linker shipped in the hal-droid tree. Same
# for /system/lib (all the libhybris-compat .so's).
do_install:append:aurora() {
    ln -sf /usr/libexec/hal-droid/system ${D}/system
}

FILES:android-system:append:aurora = " /system"
