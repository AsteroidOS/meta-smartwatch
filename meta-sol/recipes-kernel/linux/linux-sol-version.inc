# Single source of truth for sol's kernel release string. Consumed by
# linux-sol (LINUX_VERSION/PV), linux-sol-modules (PV/PKGV/KREL) and
# sol-boot-images (KMODVER) so the /lib/modules/<ver> layout, the .ipk
# filename and the workdir paths all agree by construction.
#
# sol.conf's PREFERRED_VERSION_linux-sol must be kept in sync by hand: machine
# conf is parsed before any recipe context exists, so it cannot require this.
SOL_KERNEL_VERSION = "5.15.148"
