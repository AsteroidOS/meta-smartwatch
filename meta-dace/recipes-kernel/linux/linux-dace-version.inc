# Single source of truth for dace's kernel release string. Consumed by
# linux-dace (LINUX_VERSION/PV), linux-dace-modules (PV/PKGV/KREL) and
# dace-boot-images (KMODVER) so the /lib/modules/<ver> layout, the .ipk
# filename and the workdir paths all agree by construction.
#
# NOTE: dace.conf's PREFERRED_VERSION_linux-dace must be kept in sync by
# hand — machine conf is parsed for every machine, before any recipe context
# exists, so it can't require this file.
DACE_KERNEL_VERSION = "5.15.144"
