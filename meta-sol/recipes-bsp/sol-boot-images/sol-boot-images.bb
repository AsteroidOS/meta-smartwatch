SUMMARY = "sol flashable boot artifacts: boot.img, vendor_kernel_boot.img, init_boot.img"
DESCRIPTION = "\
Builds the three .img files sol's bootloader expects, end-to-end from \
bitbake-built inputs: \
\
 - boot.img            = our linux-sol kernel Image, empty ramdisk, \
                         mkbootimg header v4 \
 - vendor_kernel_boot  = ramdisk (kernel modules per sol-vkb-modules.lst, \
                         KMI-CRC-gated against this kernel) + DTB with \
                         ramoops node injected, mkbootimg header v4 \
 - init_boot.img       = initramfs-android-image's cpio.gz, repacked as \
                         lz4, mkbootimg header v4 \
\
The base DTB is fetched as a checksummed stock input (sol-vkb-base-1.0.dtb): the kernel \
doesn't build dtbs from gki_defconfig, and sol's runtime DT is the \
bootloader-assembled one anyway."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "\
    file://sol-vkb-assemble.py \
    file://sol-vkb-modules.lst \
    file://static/modules.alias \
    file://static/modules.softdep \
    file://static/modules.load.charger \
    file://static/modules.dep \
    file://mkbootimg-aosp \
    https://mowerk.net/asteroidos/sol/sol-vkb-base-1.0.dtb;name=vkbbase \
"
SRC_URI[vkbbase.sha256sum] = "f2a4cb9e5fc25487cb0671db38c4b2aaabf8602883545f23414b5e2f163982d0"
S = "${UNPACKDIR}"

COMPATIBLE_MACHINE = "sol"

# DEPENDS:
#  - virtual/kernel: Image, Module.symvers and .ko's, read from its workdir.
#  - linux-sol-modules: techpack .ko's, read from its deployed ipk.
#  - initramfs-android-image: init_boot.img ramdisk (cpio.gz).
#  - dtc-native: fdtput. lz4-native: ramdisk compression.
#  - clang-native: llvm-strip for the vkb modules.
DEPENDS = "\
    virtual/kernel \
    linux-sol-modules \
    initramfs-android-image \
    dtc-native \
    lz4-native \
    clang-native \
"
do_compile[depends] += "\
    virtual/kernel:do_compile \
    virtual/kernel:do_install \
    linux-sol-modules:do_package_write_ipk \
    initramfs-android-image:do_image_complete \
"

PACKAGES = ""
inherit deploy nopackages

# ─── Kernel artifact paths ───
# layer.conf appends ${LAYERDIR} to BBPATH, so this require resolves from any
# recipe in meta-sol.
require recipes-kernel/linux/linux-sol-version.inc
KMODVER ?= "${SOL_KERNEL_VERSION}"
# linux-sol's workdir is sol-oe-linux-gnueabi; do_install puts .ko's in package/.
LINUX_SOL_WORKDIR ?= "${TMPDIR}/work/${MACHINE}${TARGET_VENDOR}-${TARGET_OS}/linux-sol/${KMODVER}+git"
LINUX_SOL_PKGDIR  ?= "${LINUX_SOL_WORKDIR}/package/usr/lib/modules/${KMODVER}/kernel"
# Out-of-tree build (B != S) and STAGING_KERNEL_BUILDDIR is empty, so read the
# artifacts from the workdir's build/.
LINUX_SOL_KIMG    ?= "${LINUX_SOL_WORKDIR}/build/arch/arm64/boot/Image"
LINUX_SOL_SYMVERS ?= "${LINUX_SOL_WORKDIR}/build/Module.symvers"

# Techpack ipk path. armv7vehf-neon is sol's userspace PKGARCH.
LINUX_SOL_MODULES_IPK ?= "${DEPLOY_DIR_IPK}/armv7vehf-neon/linux-sol-modules_${KMODVER}-r0_armv7vehf-neon.ipk"

# ─── mkbootimg v4 geometry ───
MKBOOTIMG_BASE          ?= "0x10000000"
MKBOOTIMG_KERNEL_OFFSET ?= "0x00008000"
MKBOOTIMG_RAMDISK_OFFSET ?= "0x01000000"
MKBOOTIMG_TAGS_OFFSET   ?= "0x00000100"
MKBOOTIMG_DTB_OFFSET    ?= "0x01f00000"
MKBOOTIMG_PAGESIZE      ?= "4096"

# ─── Hard ceilings: match the flash-slotB sanity check and deviceinfo. ───
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

    # ─── Step 1: extract techpack ipk so sol-vkb-assemble.py can find .ko's ───
    if [ ! -f "${LINUX_SOL_MODULES_IPK}" ]; then
        bbfatal "linux-sol-modules ipk not at ${LINUX_SOL_MODULES_IPK} -- check linux-sol-modules:do_deploy"
    fi
    rm -rf ${WORKDIR}/tpmod
    mkdir -p ${WORKDIR}/tpmod
    ( cd ${WORKDIR}/tpmod && ar x "${LINUX_SOL_MODULES_IPK}" && tar -xf data.tar.* )

    # ─── Step 2: assemble ramdisk dir via the python helper ───
    if [ ! -d "${LINUX_SOL_PKGDIR}" ]; then
        bbfatal "linux-sol kernel modules not at ${LINUX_SOL_PKGDIR} -- check virtual/kernel build"
    fi
    if [ ! -f "${LINUX_SOL_SYMVERS}" ]; then
        bbfatal "Module.symvers not at ${LINUX_SOL_SYMVERS}"
    fi
    rm -rf ${WORKDIR}/vkb_ramdisk
    python3 ${S}/sol-vkb-assemble.py \
        --manifest      ${S}/sol-vkb-modules.lst \
        --kernel-pkgdir "${LINUX_SOL_PKGDIR}" \
        --techpack-dir  ${WORKDIR}/tpmod \
        --symvers       "${LINUX_SOL_SYMVERS}" \
        --strip         "${STAGING_BINDIR_NATIVE}/llvm-strip" \
        --static-dir    ${S}/static \
        --out           ${WORKDIR}/vkb_ramdisk

    # ─── Step 3: ramoops DTB. Base = static vkb-base.dtb (stock,
    #            bootloader-approved with qcom,msm-id + qcom,board-id
    #            intact). Inject ramoops node so kernel pstore mounts. ───
    cp ${UNPACKDIR}/sol-vkb-base-1.0.dtb ${WORKDIR}/dtb-ramoops.dtb
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

    # ─── The se5 BT UART wakeup IRQ cannot be fixed here. vkb-base.dtb already
    #     has interrupts-extended = <0x1 0x0 0x14c 0x4>, <0x69 0x1d 0x4> (GIC SPI
    #     332 plus TLMM gpio29), but the stock DTBO replaces that node's
    #     properties and drops the second entry. New nodes (ramoops,
    #     cont-splash-fb) survive; properties on existing ones do not. ───

    # ─── Step 3b: continuous-splash framebuffer node. The bootloader paints
    #     the logo into splash_region@0x5c000000 and SDE keeps scanning it until
    #     the composer takes over. qcom-cont-splash-fb exposes that buffer as
    #     /dev/fb0 for psplash. ───
    S_NODE=/cont-splash-fb
    "${FDTPUT}" -c ${WORKDIR}/dtb-ramoops.dtb "$S_NODE"
    "${FDTPUT}" -t s ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" compatible "qcom,cont-splash-fb"
    # sol's panel is 456x456 (bootloader: "DisplayDxe: Resolution 456x456"),
    # not aurora's 384x384. The panel node (wj014sgm) lives in the dtbo; the
    # RM69090 node in vkb-base.dtb is unrelated, do not take geometry from it.
    #
    # stride = 456 * 4 = 1824, frame = 1824 * 456 = 0xCB100. The reg size must
    # be page-aligned, so 0xCC000: psplash mmaps a page-rounded length, and if
    # that exceeds smem_len the mmap fails with EINVAL and psplash exits.
    # Well inside splash_region (0xf00000).
    "${FDTPUT}" -t x ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" reg 0 0x5c000000 0 0xCC000
    "${FDTPUT}" -t u ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" width 456
    "${FDTPUT}" -t u ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" height 456
    "${FDTPUT}" -t u ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" stride 1824
    "${FDTPUT}" -t s ${WORKDIR}/dtb-ramoops.dtb "$S_NODE" format "x8r8g8b8"
    bbnote "cont-splash-fb node injected into base DTB"

    # ─── Step 4: cpio + lz4 the vkb ramdisk ───
    ( cd ${WORKDIR}/vkb_ramdisk && find . | sort | \
        cpio -o -H newc --owner root:root 2>/dev/null ) > ${WORKDIR}/vkb_rd.cpio
    lz4 -l -9 -f ${WORKDIR}/vkb_rd.cpio ${WORKDIR}/vkb_rd.lz4

    MKBOOTIMG="python3 ${S}/mkbootimg-aosp/mkbootimg.py"

    # ─── Step 5: mkbootimg vendor_kernel_boot.img (v4) ───
    ${MKBOOTIMG} \
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
    if [ ! -f "${LINUX_SOL_KIMG}" ]; then
        bbfatal "Kernel Image not at ${LINUX_SOL_KIMG}"
    fi
    : > ${WORKDIR}/empty_kernel
    : > ${WORKDIR}/empty_ramdisk
    ${MKBOOTIMG} \
        --header_version 4 \
        --kernel "${LINUX_SOL_KIMG}" \
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
    ${MKBOOTIMG} \
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
    ( cd ${DEPLOYDIR} && sha256sum boot.img vendor_kernel_boot.img init_boot.img > SHA256SUMS-sol )
    bbnote "sol boot artifacts deployed: ${DEPLOYDIR}/{boot,vendor_kernel_boot,init_boot}.img"
}
addtask deploy after do_compile before do_build
