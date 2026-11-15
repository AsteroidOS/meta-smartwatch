# Trigger sol-boot-images (boot.img, vendor_kernel_boot.img, init_boot.img) as a
# side-effect of `bitbake asteroid-image`, gated on MACHINE: every meta-smartwatch
# sublayer is in bblayers for all machines, so an unconditional dep would make
# other machines demand sol-boot-images (COMPATIBLE_MACHINE-skipped) and fail.
do_image_complete[depends] += "${@bb.utils.contains('MACHINE', 'sol', 'sol-boot-images:do_build', '', d)}"
