SUMMARY = "Aurora flashable boot artifacts: boot.img, vendor_kernel_boot.img, init_boot.img"
DESCRIPTION = "\
Builds the three .img files aurora's bootloader expects, end-to-end from \
bitbake-built inputs: \
\
 - boot.img            = our linux-aurora kernel Image, empty ramdisk, \
                         mkbootimg header v4 \
 - vendor_kernel_boot  = ramdisk (kernel modules per aurora-vkb-modules.lst, \
                         KMI-CRC-gated against this kernel) + DTB with \
                         ramoops node injected, mkbootimg header v4 \
 - init_boot.img       = initramfs-android-image's cpio.gz, repacked as \
                         lz4, mkbootimg header v4 \
\
The base DTB is shipped as a static input (vkb-base.dtb) -- the kernel \
doesn't build dtbs from gki_defconfig, and aurora's runtime DT is the \
bootloader-assembled one anyway."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "\
    file://aurora-vkb-assemble.py \
    file://aurora-vkb-modules.lst \
    file://static/modules.alias \
    file://static/modules.softdep \
    file://static/modules.load.charger \
    file://static/modules.dep \
    file://static/vkb-base.dtb \
"
S = "${UNPACKDIR}"

COMPATIBLE_MACHINE = "aurora"

# DEPENDS:
#  - virtual/kernel: provides Image + Module.symvers + kernel-built .ko's
#                    (we read them out of the linux-aurora workdir).
#  - linux-aurora-modules: techpack .ko's; we read its deploy ipk.
#  - initramfs-android-image: init_boot.img ramdisk content (cpio.gz).
#  - mkbootimg-tools-native: provides ${STAGING_BINDIR_NATIVE}/mkbootimg
#  - dtc-native: provides ${STAGING_BINDIR_NATIVE}/fdtput
DEPENDS = "\
    virtual/kernel \
    linux-aurora-modules \
    initramfs-android-image \
    mkbootimg-tools-native \
    dtc-native \
"
do_compile[depends] += "\
    virtual/kernel:do_compile \
    virtual/kernel:do_install \
    linux-aurora-modules:do_package_write_ipk \
    initramfs-android-image:do_image_complete \
"

PACKAGES = ""
inherit deploy nopackages

# ─── Kernel artifact paths ───
# linux-aurora builds out-of-tree: source in STAGING_KERNEL_DIR,
# .config/Image/Module.symvers in STAGING_KERNEL_BUILDDIR. .ko's install into
# the recipe's package/ via kernel.bbclass.
# layer.conf appends ${LAYERDIR} to BBPATH, so this layer-root-relative
# require resolves from any recipe in meta-aurora.
require recipes-kernel/linux/linux-aurora-version.inc
KMODVER ?= "${AURORA_KERNEL_VERSION}"
# linux-aurora's workdir uses ${MACHINE}${TARGET_VENDOR}-${TARGET_OS}
# (= aurora-oe-linux-gnueabi). do_install drops .ko's into package/.
LINUX_AURORA_WORKDIR ?= "${TMPDIR}/work/${MACHINE}${TARGET_VENDOR}-${TARGET_OS}/linux-aurora/${KMODVER}+git"
LINUX_AURORA_PKGDIR  ?= "${LINUX_AURORA_WORKDIR}/package/usr/lib/modules/${KMODVER}/kernel"
# linux-aurora keeps its build artifacts in its own workdir's build/
# (out-of-tree B != S). STAGING_KERNEL_BUILDDIR is empty because we don't go
# through the standard kernel.bbclass staging for these.
LINUX_AURORA_KIMG    ?= "${LINUX_AURORA_WORKDIR}/build/arch/arm64/boot/Image"
LINUX_AURORA_SYMVERS ?= "${LINUX_AURORA_WORKDIR}/build/Module.symvers"

# Techpack ipk path. armv7vehf-neon is aurora's userspace PKGARCH.
LINUX_AURORA_MODULES_IPK ?= "${DEPLOY_DIR_IPK}/armv7vehf-neon/linux-aurora-modules_${KMODVER}-r0_armv7vehf-neon.ipk"

# ─── mkbootimg v4 geometry ───
MKBOOTIMG_BASE          ?= "0x10000000"
MKBOOTIMG_KERNEL_OFFSET ?= "0x00008000"
MKBOOTIMG_RAMDISK_OFFSET ?= "0x01000000"
MKBOOTIMG_TAGS_OFFSET   ?= "0x00000100"
MKBOOTIMG_DTB_OFFSET    ?= "0x01f00000"
MKBOOTIMG_PAGESIZE      ?= "4096"

# ─── Hard ceilings -- match flash-slotB sanity check + deviceinfo. ───
BOOT_IMG_CAP                   ?= "67108864"
VENDOR_KERNEL_BOOT_IMG_CAP     ?= "67108864"
INIT_BOOT_IMG_CAP              ?= "8388608"

# ─── Ramoops region (matches stock dtbo) ───
RAMOOPS_BASE      ?= "0x61f00000"
RAMOOPS_SIZE      ?= "0x400000"
RAMOOPS_RECORD    ?= "0x40000"
RAMOOPS_CONSOLE   ?= "0x200000"
RAMOOPS_PMSG      ?= "0x100000"

do_compile() {
    set -e

    # ─── Step 1: extract techpack ipk so aurora-vkb-assemble.py can find .ko's ───
    if [ ! -f "${LINUX_AURORA_MODULES_IPK}" ]; then
        bbfatal "linux-aurora-modules ipk not at ${LINUX_AURORA_MODULES_IPK} -- check linux-aurora-modules:do_deploy"
    fi
    rm -rf ${WORKDIR}/tpmod
    mkdir -p ${WORKDIR}/tpmod
    ( cd ${WORKDIR}/tpmod && ar x "${LINUX_AURORA_MODULES_IPK}" && tar -xf data.tar.* )

    # ─── Step 2: assemble ramdisk dir via the python helper ───
    if [ ! -d "${LINUX_AURORA_PKGDIR}" ]; then
        bbfatal "linux-aurora kernel modules not at ${LINUX_AURORA_PKGDIR} -- check virtual/kernel build"
    fi
    if [ ! -f "${LINUX_AURORA_SYMVERS}" ]; then
        bbfatal "Module.symvers not at ${LINUX_AURORA_SYMVERS}"
    fi
    rm -rf ${WORKDIR}/vkb_ramdisk
    python3 ${S}/aurora-vkb-assemble.py \
        --manifest      ${S}/aurora-vkb-modules.lst \
        --kernel-pkgdir "${LINUX_AURORA_PKGDIR}" \
        --techpack-dir  ${WORKDIR}/tpmod \
        --symvers       "${LINUX_AURORA_SYMVERS}" \
        --static-dir    ${S}/static \
        --out           ${WORKDIR}/vkb_ramdisk

    # ─── Step 3: ramoops DTB. Base = static vkb-base.dtb (stock,
    #            bootloader-approved with qcom,msm-id + qcom,board-id
    #            intact). Inject ramoops node so kernel pstore mounts. ───
    cp ${S}/static/vkb-base.dtb ${WORKDIR}/dtb-ramoops.dtb
    FDTPUT=${STAGING_BINDIR_NATIVE}/fdtput
    N=/reserved-memory/ramoops@61F00000
    "${FDTPUT}" -c ${WORKDIR}/dtb-ramoops.dtb "$N"
    "${FDTPUT}" -t s ${WORKDIR}/dtb-ramoops.dtb "$N" compatible ramoops
    "${FDTPUT}"      ${WORKDIR}/dtb-ramoops.dtb "$N" no-map
    "${FDTPUT}" -t x ${WORKDIR}/dtb-ramoops.dtb "$N" reg 0 ${RAMOOPS_BASE} 0 ${RAMOOPS_SIZE}
    "${FDTPUT}" -t x ${WORKDIR}/dtb-ramoops.dtb "$N" record-size ${RAMOOPS_RECORD}
    "${FDTPUT}" -t x ${WORKDIR}/dtb-ramoops.dtb "$N" console-size ${RAMOOPS_CONSOLE}
    "${FDTPUT}" -t x ${WORKDIR}/dtb-ramoops.dtb "$N" pmsg-size ${RAMOOPS_PMSG}
    bbnote "ramoops node injected into base DTB"

    # ─── Step 3b: continuous-splash framebuffer node. The bootloader paints
    #     the boot logo into splash_region@0x5c000000 and SDE keeps scanning it
    #     until the composer takes over. qcom-cont-splash-fb binds this node and
    #     exposes that live buffer as /dev/fb0 for psplash. Geometry is the
    #     panel native res (384x384, 32bpp xRGB, stride 384*4=1536); frame =
    #     1536*384 = 0x90000 bytes. ───
    S_NODE=/cont-splash-fb
    "${FDTPUT}" -c ${WORKDIR}/dtb-ramoops.dtb "$S_NODE"
    "${FDTPUT}" -t s ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" compatible "qcom,cont-splash-fb"
    "${FDTPUT}" -t x ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" reg 0 0x5c000000 0 0x90000
    "${FDTPUT}" -t u ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" width 384
    "${FDTPUT}" -t u ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" height 384
    "${FDTPUT}" -t u ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" stride 1536
    "${FDTPUT}" -t s ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" format "x8r8g8b8"
    bbnote "cont-splash-fb node injected into base DTB"

    # ─── Step 4: cpio + lz4 the vkb ramdisk ───
    ( cd ${WORKDIR}/vkb_ramdisk && find . | sort | \
        cpio -o -H newc --owner root:root 2>/dev/null ) > ${WORKDIR}/vkb_rd.cpio
    lz4 -l -9 -f ${WORKDIR}/vkb_rd.cpio ${WORKDIR}/vkb_rd.lz4

    MKBOOTIMG=${STAGING_BINDIR_NATIVE}/mkbootimg

    # ─── Step 5: mkbootimg vendor_kernel_boot.img (v4) ───
    "${MKBOOTIMG}" \
        --header_version 4 --pagesize ${MKBOOTIMG_PAGESIZE} \
        --vendor_boot ${WORKDIR}/vendor_kernel_boot.img \
        --vendor_ramdisk ${WORKDIR}/vkb_rd.lz4 \
        --dtb ${WORKDIR}/dtb-ramoops.dtb \
        --base ${MKBOOTIMG_BASE} \
        --kernel_offset ${MKBOOTIMG_KERNEL_OFFSET} \
        --ramdisk_offset ${MKBOOTIMG_RAMDISK_OFFSET} \
        --tags_offset ${MKBOOTIMG_TAGS_OFFSET} \
        --dtb_offset ${MKBOOTIMG_DTB_OFFSET} \
        --vendor_cmdline ''

    # ─── Step 6: mkbootimg boot.img (v4): our kernel + empty ramdisk ───
    if [ ! -f "${LINUX_AURORA_KIMG}" ]; then
        bbfatal "Kernel Image not at ${LINUX_AURORA_KIMG}"
    fi
    : > ${WORKDIR}/empty_kernel
    : > ${WORKDIR}/empty_ramdisk
    "${MKBOOTIMG}" \
        --header_version 4 \
        --kernel "${LINUX_AURORA_KIMG}" \
        --ramdisk ${WORKDIR}/empty_ramdisk \
        --cmdline '' \
        -o ${WORKDIR}/boot.img

    # ─── Step 7: mkbootimg init_boot.img (v4): asteroid initramfs as lz4 ───
    CPIO_GZ=$(ls -t ${DEPLOY_DIR_IMAGE}/initramfs-android-image-${MACHINE}-*.cpio.gz 2>/dev/null | grep -v debug | head -1)
    if [ -z "$CPIO_GZ" ] || [ ! -f "$CPIO_GZ" ]; then
        CPIO_GZ=${DEPLOY_DIR_IMAGE}/initramfs-android-image-${MACHINE}.cpio.gz
    fi
    if [ ! -f "$CPIO_GZ" ]; then
        bbfatal "initramfs-android-image cpio.gz not found in ${DEPLOY_DIR_IMAGE}"
    fi
    gunzip -c "$CPIO_GZ" | lz4 -l -9 > ${WORKDIR}/init_boot_rd.lz4
    "${MKBOOTIMG}" \
        --header_version 4 \
        --kernel ${WORKDIR}/empty_kernel \
        --ramdisk ${WORKDIR}/init_boot_rd.lz4 \
        --cmdline '' \
        -o ${WORKDIR}/init_boot.img

    # ─── Step 8: size-cap sanity check ───
    check_cap() {
        local f=$1 cap=$2
        local sz=$(stat -c%s "$f")
        if [ "$sz" -gt "$cap" ]; then
            bbfatal "$(basename $f) is ${sz}B > cap ${cap}B"
        fi
        bbnote "$(basename $f): ${sz}B (cap ${cap}B) OK"
    }
    check_cap ${WORKDIR}/boot.img                ${BOOT_IMG_CAP}
    check_cap ${WORKDIR}/vendor_kernel_boot.img  ${VENDOR_KERNEL_BOOT_IMG_CAP}
    check_cap ${WORKDIR}/init_boot.img           ${INIT_BOOT_IMG_CAP}
}

do_deploy() {
    install -d ${DEPLOYDIR}
    for f in boot.img vendor_kernel_boot.img init_boot.img; do
        install -m 0644 ${WORKDIR}/$f ${DEPLOYDIR}/$f
    done
    ( cd ${DEPLOYDIR} && sha256sum boot.img vendor_kernel_boot.img init_boot.img > SHA256SUMS-aurora )
    bbnote "Aurora boot artifacts deployed: ${DEPLOYDIR}/{boot,vendor_kernel_boot,init_boot}.img"
}
addtask deploy after do_compile before do_build
