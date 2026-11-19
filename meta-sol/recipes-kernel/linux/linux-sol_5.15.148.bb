require recipes-kernel/linux/linux.inc

SUMMARY = "Pixel Watch 3 (sol) kernel: Google's msm 5.15 (seluna, Android 14 Wear)"
HOMEPAGE = "https://android.googlesource.com/kernel/msm"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"
COMPATIBLE_MACHINE = "sol"

# Pixel Watch 3 (sol) kernel, built from source. do_configure:prepend
# generates the .config and stages it as ${WORKDIR}/defconfig for linux.inc.
SRC_URI = "git://android.googlesource.com/kernel/msm;branch=android-msm-seluna-5.15-android14-wear;protocol=https \
           file://sw5100.fragment \
           file://eud-secure-fail-nonfatal.patch \
           file://rtc-pm8xxx-read-only.patch \
           file://geni-serial-resume-on-tx.patch \
           file://geni-serial-wakelock-only-on-rx.patch \
           file://geni-serial-force-suspend-like-spi.patch \
           file://0001-video-fbdev-add-qcom-continuous-splash-framebuffer.patch \
           file://pm-runtime-set-status-atomic.patch \
           file://vhci-no-suspend-notifier.patch"
SRCREV = "6c2216b229e8427961c29bdc5c3bf95088d9615c"
require linux-sol-version.inc
LINUX_VERSION = "${SOL_KERNEL_VERSION}"
PV = "${SOL_KERNEL_VERSION}+git${SRCPV}"
S = "${UNPACKDIR}/${BP}"
# Out-of-tree build (B != S) is required: scripts/Makefile.lib adds
# `-I $(srctree)/$(src)` only when building out of srctree, and vendor drivers
# such as WALT (`#include <walt.h>`) and CREATE_TRACE_POINTS users need it.
B = "${WORKDIR}/build"

ARCH = "arm64"
KERNEL_IMAGETYPE = "Image"

# The real sw5100_gki_defconfig (build.config.sw5100): gki_defconfig +
# vendor/monaco_GKI.config + sw5100.fragment, merged with merge_config.sh.
# A bare GKI defconfig leaves every SoC driver off. BTF is off because
# linux.inc does not provide native libelf/pahole for resolve_btfids.
do_configure:prepend() {
    sh ${S}/scripts/kconfig/merge_config.sh -m -r -O ${WORKDIR} \
        ${S}/arch/arm64/configs/gki_defconfig \
        ${S}/arch/arm64/configs/vendor/monaco_GKI.config \
        ${UNPACKDIR}/sw5100.fragment
    mv ${WORKDIR}/.config ${WORKDIR}/defconfig
    echo "# CONFIG_DEBUG_INFO_BTF is not set" >> ${WORKDIR}/defconfig
    echo "# CONFIG_DEBUG_INFO_BTF_MODULES is not set" >> ${WORKDIR}/defconfig
    # gki_defconfig sets CONFIG_WERROR=y, which turns benign vendor-tree
    # warnings fatal.
    echo "# CONFIG_WERROR is not set" >> ${WORKDIR}/defconfig
    # Avoid a ThinLTO vmlinux-link OOM on the full sw5100 config.
    echo "# CONFIG_LTO_CLANG_THIN is not set" >> ${WORKDIR}/defconfig
    echo "# CONFIG_LTO_CLANG_FULL is not set" >> ${WORKDIR}/defconfig
    echo "# CONFIG_LTO_CLANG is not set" >> ${WORKDIR}/defconfig
    echo "CONFIG_LTO_NONE=y" >> ${WORKDIR}/defconfig
    echo "# CONFIG_CFI_CLANG is not set" >> ${WORKDIR}/defconfig
    # SoC/vendor drivers stay =m. Many resolve symbols only at module load
    # (e.g. qcom_smd_rpm_quickboot comes from a provider .ko), so forcing =y
    # fails to link with `ld.lld: undefined symbol`.
}

# meta-clang's clang builds the kernel and all vendor modules. The kernel and
# linux-sol-modules must use the same toolchain so vermagic and CRCs match.
# LLVM=1 LLVM_IAS=1, so CROSS_COMPILE is vestigial.
DEPENDS += "clang-native lld-native rsync-native"
LLVM_BIN = "${STAGING_BINDIR_NATIVE}"
PATH:prepend = "${STAGING_BINDIR_NATIVE}:"
# icecc.bbclass builds its wrapper symlink from the first word of KERNEL_CC by
# string concatenation; an absolute path there yields a symlink path whose
# parent dirs do not exist, and do_configure dies with FileNotFoundError.
KERNEL_CC = "${LLVM_BIN}/clang --target=aarch64-linux-gnu -fuse-ld=lld"

# icecream cannot distribute the kernel compiles: fixdep needs the -Wp,-MD
# dependency file locally, and a remote compile never writes it:
#   fixdep: error opening file: scripts/mod/.empty.o.d: No such file or directory
ICECC_DISABLED = "1"
KERNEL_LD = "${LLVM_BIN}/ld.lld"
KERNEL_AR = "${LLVM_BIN}/llvm-ar"
KERNEL_NM = "${LLVM_BIN}/llvm-nm"
KERNEL_OBJCOPY = "${LLVM_BIN}/llvm-objcopy"
KERNEL_OBJDUMP = "${LLVM_BIN}/llvm-objdump"
KERNEL_STRIP = "${LLVM_BIN}/llvm-strip"
EXTRA_OEMAKE:append = " LLVM=1 LLVM_IAS=1 ARCH=arm64 CROSS_COMPILE=aarch64-linux-gnu-"
# clang 15-20 made a family of C89-isms default errors (implicit-int,
# int-conversion, incompatible pointer types, implicit function declarations,
# strict-prototypes). This vendor tree was written for clang 14 and has many,
# e.g. `static md_align_offset;` in minidump_log.c. Downgrade them to warnings.
EXTRA_OEMAKE:append = " KCFLAGS='-Wno-error -Wno-implicit-function-declaration -Wno-error=implicit-function-declaration -Wno-error=implicit-int -Wno-error=int-conversion -Wno-error=incompatible-function-pointer-types -Wno-error=incompatible-pointer-types -Wno-error=strict-prototypes' HOSTCFLAGS=-Wno-error"

# do_package debug-split/strip uses recipe (not KERNEL_) binutils; the armv7
# defaults choke on the aarch64 vmlinux/modules.
OBJCOPY:class-target = "${LLVM_BIN}/llvm-objcopy"
STRIP:class-target = "${LLVM_BIN}/llvm-strip"
NM:class-target = "${LLVM_BIN}/llvm-nm"
AR:class-target = "${LLVM_BIN}/llvm-ar"

# The bootloader supplies the DTB and the kernel has a built-in CONFIG_CMDLINE,
# so only Image is emitted; it is repacked into boot.img off-line.
KERNEL_DEVICETREE = ""

ERROR_QA:remove = "arch buildpaths"
WARN_QA:append = " arch buildpaths"

# systempart/datapart are explicit in case the bootloader omits them on a slot.
#
# lsm=: SELinux instead of AppArmor. Android init calls setexeccon() for every
# service with a seclabel and forks the /vendor subcontext unconditionally.
# Without selinuxfs those fail with EINVAL and no Android service starts.
# CONFIG_LSM lists apparmor first, and the two are exclusive. Nothing here uses
# AppArmor. No policy is loaded: enforcing=0 and androidboot.selinux=permissive
# only let setexeccon succeed.
SOL_CMDLINE = "systempart=/dev/mapper/system datapart=/dev/mmcblk0p81 console=tty0 lsm=capability,landlock,lockdown,yama,loadpin,safesetid,integrity,selinux,bpf enforcing=0 androidboot.selinux=permissive"
do_configure:append() {
    sed -i '/^CONFIG_CMDLINE=/d' ${B}/.config
    echo 'CONFIG_CMDLINE="${SOL_CMDLINE}"' >> ${B}/.config
    oe_runmake -C ${S} O=${B} ARCH=arm64 \
        CC="${LLVM_BIN}/clang --target=aarch64-linux-gnu -fuse-ld=lld" \
        LD="${LLVM_BIN}/ld.lld" LLVM=1 LLVM_IAS=1 \
        CROSS_COMPILE=aarch64-linux-gnu- HOSTCC=gcc HOSTCXX=g++ olddefconfig
    grep '^CONFIG_CMDLINE=' ${B}/.config
}

do_install:append() {
    rm -rf ${D}/usr/src/usr/
    find ${D}/usr/src/ -name ..install.cmd -delete 2>/dev/null || true
}
