do_configure:hoki() {
    # Override the targets to be used before compiling kernel modules.
    # Remove the 'prepare' target as it would fail.
    unset CFLAGS CPPFLAGS CXXFLAGS LDFLAGS
    oe_runmake CC="${KERNEL_CC}" LD="${KERNEL_LD}" AR="${KERNEL_AR}" \
    -C ${STAGING_KERNEL_DIR} O=${STAGING_KERNEL_BUILDDIR} scripts
}
