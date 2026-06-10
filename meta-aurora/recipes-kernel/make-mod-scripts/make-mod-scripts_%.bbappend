# Override do_configure for aurora's aarch64 kernel cross-build. The upstream
# task runs `make prepare scripts`, but our tree's `prepare` target fails (the
# kernel is already configured for our cross-tune); we only need `scripts`. Use
# KERNEL_* vars so the right cross-tools are picked up.
do_configure:aurora() {
    unset CFLAGS CPPFLAGS CXXFLAGS LDFLAGS
    oe_runmake CC="${KERNEL_CC}" LD="${KERNEL_LD}" \
    AR="${KERNEL_AR}" OBJCOPY="${KERNEL_OBJCOPY}" \
    -C ${STAGING_KERNEL_DIR} O=${STAGING_KERNEL_BUILDDIR} scripts
}
