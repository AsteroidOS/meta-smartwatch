# Build the three flashable boot artifacts (boot.img, vendor_kernel_boot.img,
# init_boot.img) as a side-effect of `bitbake asteroid-image`. Guard on
# MACHINE: meta-aurora is in bblayers for every machine (update_layer_config
# auto-includes all meta-smartwatch sublayers), and an unconditional dep
# would make every other machine's asteroid-image demand aurora-boot-images
# (COMPATIBLE_MACHINE-skipped there) and fail.
do_image_complete[depends] += "${@bb.utils.contains('MACHINE', 'aurora', 'aurora-boot-images:do_build', '', d)}"
