# Single source of truth for aurora's kernel release string. Consumed by
# linux-aurora (LINUX_VERSION/PV), linux-aurora-modules (PV/PKGV/KREL) and
# aurora-boot-images (KMODVER) so the /lib/modules/<ver> layout, the .ipk
# filename and the workdir paths all agree by construction.
#
# NOTE: aurora.conf's PREFERRED_VERSION_linux-aurora must be kept in sync by
# hand — machine conf is parsed for every machine, before any recipe context
# exists, so it can't require this file.
AURORA_KERNEL_VERSION = "5.15.144"
