SUMMARY = "Pixel Watch 3 (sol) Qualcomm/Google vendor kernel modules"
DESCRIPTION = "Builds sol's vendor kernel modules (securemsm, nanohub, \
mcu_display, audio, display, graphics, bms, the qcom charger/fuel gauge, \
extcon shim, touch, Broadcom BT and WLAN, mcu_mic_codec, cs40l26 haptics) \
from Google's sources against linux-sol with the same clang and config, so \
vermagic, CRCs and ABI match by construction. Every repo is pinned; the \
branch is android-msm-seluna-5.15-android14-wear unless noted."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"
COMPATIBLE_MACHINE = "sol"
# OE appends "+<every SRCREV>" to PKGV for multi-SRCREV git recipes, which
# pushed the .ipk filename past NAME_MAX. Pin PKGV; SRCREV_<name> still pins
# every repo and task hashes still track it.
require linux-sol-version.inc
PV = "${SOL_KERNEL_VERSION}"
PKGV = "${SOL_KERNEL_VERSION}"

# Module repos at the private/ paths build.config.sw5100 and the
# kernel-platform manifest expect.
AGM = "git://android.googlesource.com/kernel"
S = "${UNPACKDIR}/src"
SRC_URI = "\
  ${AGM}/msm-extra;protocol=https;branch=${KBR};name=audio;destsuffix=src/private/msm-google-modules/audio \
  ${AGM}/msm-extra/display-drivers;protocol=https;branch=${KBR};name=display;destsuffix=src/private/msm-google-modules/display \
  ${AGM}/msm-modules/graphics;protocol=https;branch=${KBR};name=graphics;destsuffix=src/private/msm-google-modules/graphics \
  ${AGM}/msm-modules/securemsm;protocol=https;branch=${KBR};name=securemsm;destsuffix=src/private/msm-google-modules/securemsm \
  ${AGM}/google-modules/soc/msm;protocol=https;branch=android-msm-seluna-5.15-android14-wear;name=socmsm;destsuffix=src/private/google-modules/soc/msm \
  ${AGM}/google-modules/bms;protocol=https;branch=${KBR};name=bms;destsuffix=src/private/google-modules/bms \
  ${AGM}/google-modules/touch/novatek_touch;protocol=https;branch=${KBR};name=nvttouch;destsuffix=src/private/google-modules/touch/novatek \
  ${AGM}/google-modules/nanohub;protocol=https;branch=android-msm-seluna-5.15-android14-wear;name=nanohub;destsuffix=src/private/google-modules/nanohub \
  ${AGM}/google-modules/sound/mcu_mic_codec;protocol=https;branch=android-msm-seluna-5.15-android14-wear;name=mcumic;destsuffix=src/private/google-modules/sound/mcu_mic_codec \
  ${AGM}/google-modules/amplifiers;protocol=https;branch=android-msm-eos-5.15-tm-wear-kr3-pixel-watch;name=amplifiers;destsuffix=src/private/google-modules/amplifiers \
  ${AGM}/google-modules/display/watch/mcu-offload;protocol=https;branch=${KBR};name=mcudisp;destsuffix=src/private/google-modules/display/watch/mcu-offload \
  ${AGM}/google-modules/bluetooth/broadcom;protocol=https;branch=${KBR};name=btbrcm;destsuffix=src/private/google-modules/bluetooth/broadcom \
  ${AGM}/google-modules/wlan/syna/dhd4381;protocol=https;branch=${KBR};name=synadhd;destsuffix=src/private/google-modules/wlan/syna/dhd4381 \
  file://mcu_display-Kbuild \
  file://0004-amplifiers-cs40l26-set-loaded-before-mfd-add-devices.patch \
  file://0005-amplifiers-cl_dsp-return-ENOENT-when-coeff-control-i.patch \
  file://0006-amplifiers-cs40l26-pin-codec-MFD-child-platform-id.patch \
  file://0007-amplifiers-cs40l26-drive-reset-by-physical-level.patch \
  file://0001-audio-monaco-bind-haptics-codec-by-deterministic-MFD.patch \
  file://0008-amplifiers-cs40l26-codec-report-state-to-monaco-asoc.patch \
  file://0010-display-sde-connector-show-the-always-on-face-on-sol.patch \
  file://0011-touch-nt383x0-report-tap-to-wake-as-a-power-key-on-sol.patch \
  file://sol_vidc_stub.c \
  file://sol-vidc-stub-Kbuild \
"
KBR = "android-msm-seluna-5.15-android14-wear"
# Every repo is pinned: three sol patches apply to these trees, and a moving
# revision builds different modules under the same package version. The
# connectivity revisions were found via Google's kernel manifest, branch
# android-msm-seluna-5.15-android14-qpr3 (no "-wear").
SRCREV_btbrcm = "30c9f3d04c698274f234c67afba82c19d2c5ca42"
SRCREV_synadhd = "b2da16bf1b0523c2e8c35e3cf2bcd47d608da6df"
SRCREV_audio = "2ee909f8d67c1418e84c2277476e5c9877a3f11a"
SRCREV_display = "bd5ba36e342a13247152598964f163d4b387e038"
SRCREV_graphics = "df85c99c4978e19c47a740ff616bad125a8756e2"
SRCREV_securemsm = "8c28e44bf54c65f6190dafeb6fef47a2a3e49092"
SRCREV_nvttouch = "e70ddd02dcc8ef99af37c6bc8742014fb7bbb2d9"
# socmsm carries usb_shim (google,extcon-usb-shim).
SRCREV_socmsm = "4f6333d68d301b7eeaaec45d179cbc79b325beff"
SRCREV_bms = "37a180d01e9d6f492f131959a43d576369c2fa5c"
# nanohub (sensor-hub MCU driver). Its CONFIG_NANOHUB_* gates are in no
# Kconfig; see modroot and KMOD_MAKE.
SRCREV_nanohub = "3b5b064165681466aaab51984e88cc58bc3ed14d"
# mcu_mic_codec: the ASoC codec on nanohub's audio channel.
SRCREV_mcumic = "cf0ae0b466edeb0ff42185b701a9dfcabf054f30"
# Pinned to aurora's revision: sol's seluna branch is bazel-only and its
# cs40l26 does not bind here. Same CS40L26A (chip ID 0x40A260).
SRCREV_amplifiers = "06552df7596558c5b9b0fe1ad39fefc4ae116ef7"
SRCREV_mcudisp = "501680071c16f38dd3cbfdc5ff079965ad2a3fba"
SRCREV_FORMAT = "audio_display_graphics_securemsm_socmsm_bms_nvttouch_nanohub_mcumic_amplifiers_mcudisp_btbrcm_synadhd"


# Build against the exact linux-sol kernel (same clang and config, so vermagic,
# CRCs and ABI match) plus the host tools modules_prepare needs.
DEPENDS += "virtual/kernel clang-native lld-native"
DEPENDS += "bc-native bison-native flex-native openssl-native kmod-native rsync-native coreutils-native"
do_configure[depends] += "virtual/kernel:do_shared_workdir"

LLVM_BIN = "${STAGING_BINDIR_NATIVE}"
PATH:prepend = "${STAGING_BINDIR_NATIVE}:"
# linux-sol builds out of tree: source in STAGING_KERNEL_DIR, .config,
# Module.symvers and genksyms in STAGING_KERNEL_BUILDDIR.
KDIR = "${STAGING_KERNEL_DIR}"
KOUT = "${STAGING_KERNEL_BUILDDIR}"

# EXT_MODULES build order from build.config.sw5100 (M= dirs relative to S).
#
# Order matters: do_compile's $extra accumulator offers a module only the
# Module.symvers of the modules before it. The display chain is
# nanohub <- mcu_display <- msm_drm (mcu_display calls into nanohub;
# dsi_display.c calls mcu_display_register_panel_power_notifier()).
#
# Not built, because sol does not need them to boot or lacks the hardware:
#   video, bt, mm, mmrm, dataipa, datarmnet, datarmnet-ext, Qualcomm WLAN
#   (sol's is Broadcom), rotary-encoders. Their repos are not fetched.
#
# WLAN and BT are Synaptics/Broadcom on sol, not Qualcomm as on aurora: the DT
# has /soc/wlan-gpios "android,syna_wlan" and /soc/btbcm "goog,nitrous", and
# stock loads synadhd4381.ko with BCM4381A1.hcd / fw_bcmdhd.bin. rfkill and
# btbcm are built in (CONFIG_RFKILL=y, CONFIG_BT_BCM=y in sw5100.fragment).
# Firmware comes from sol-connectivity-firmware; the stock modules themselves
# cannot be reused (CRC mismatches, CFI symbols this kernel does not export).
EXT_MODULES = "\
  private/msm-google-modules/securemsm \
  private/google-modules/nanohub \
  private/google-modules/display/watch/mcu-offload/mcu_display \
  private/msm-google-modules/audio \
  private/msm-google-modules/display \
  private/msm-google-modules/graphics \
  private/google-modules/bms/misc \
  private/google-modules/bms \
  private/msm-google/drivers/power/supply/qcom \
  private/google-modules/soc/msm/drivers/extcon/google \
  private/google-modules/touch/novatek/nt383x0 \
  private/google-modules/bluetooth/broadcom \
  private/google-modules/wlan/syna/dhd4381 \
  private/google-modules/sound/mcu_mic_codec \
  private/google-modules/amplifiers/cs40l26 \
"

KMOD_MAKE = "make -C ${KDIR} O=${KOUT} ARCH=arm64 LLVM=1 LLVM_IAS=1 \
  CROSS_COMPILE=aarch64-linux-gnu- \
  CC='${LLVM_BIN}/clang --target=aarch64-linux-gnu -fuse-ld=lld' \
  LD='${LLVM_BIN}/ld.lld' AR='${LLVM_BIN}/llvm-ar' \
  NM='${LLVM_BIN}/llvm-nm' OBJCOPY='${LLVM_BIN}/llvm-objcopy' \
  KERNEL_SRC=${KDIR} OUT_DIR=${KDIR} \
  KCFLAGS='-Wno-error -Wno-error=implicit-int -Wno-error=int-conversion -Wno-error=incompatible-function-pointer-types -Wno-error=incompatible-pointer-types -Wno-error=implicit-function-declaration -Wno-error=strict-prototypes -I${KDIR}/drivers/clk/qcom -I${KDIR}/drivers/devfreq -I${KDIR}/include/linux -I${KDIR}/drivers/iommu -I${S}/private/google-modules/soc/msm/include/dtc -I${S}/private/google-modules/nanohub -DCONFIG_NANOHUB -DCONFIG_NANOHUB_SPI -DCONFIG_NANOHUB_BL_NXP -DCONFIG_NANOHUB_DISPLAY' \
  KBUILD_MODPOST_WARN=1 CONFIG_ARCH_MONACO=y CONFIG_TOUCHSCREEN_NT383x0=m"
# KERNEL_SRC / OUT_DIR: the bms and power/supply/qcom wrapper Makefiles include
# $(KERNEL_SRC)/../google-modules/soc/msm/Makefile.include and chain
# $(OUT_DIR)/../google-modules/bms/misc/Module.symvers. Both default to the host
# kernel; ${KDIR}'s parent holds the module-tree symlinks made in do_compile.

# The machine is armv7, so default binutils choke on the aarch64 .ko; use
# llvm tools and do not strip.
OBJCOPY:class-target = "${LLVM_BIN}/llvm-objcopy"
STRIP:class-target = "${LLVM_BIN}/llvm-strip"
INHIBIT_SYSROOT_STRIP = "1"

do_configure[noexec] = "1"

# Each Qualcomm Kbuild expects a <NAME>_ROOT var pointing at its repo root.
# They concatenate without a separator (`-I$(SYNC_FENCE_ROOT)sync_fence/...`),
# so every *_ROOT value must end with a slash.
modroot() {
    P="${S}/private/msm-google-modules"
    case "$1" in
      securemsm)            echo "SSG_MODULE_ROOT=$P/securemsm/" ;;
      # Audio needs MODNAME non-empty (external-build path) and
      # CONFIG_ARCH_MONACO=y; together they include config/sw5100auto.conf and
      # sw5100autoconf.h. Without them no audio .ko builds, including
      # adsp_loader_dlkm, which provides /sys/kernel/boot_adsp/boot. The modem
      # also needs the ADSP up (otherwise its watchdog reports "DOG detects
      # stalled initialization").
      audio)                echo "AUDIO_ROOT=$P/audio/ MODNAME=audio CONFIG_ARCH_MONACO=y" ;;
      # Display needs both gates, and neither is in any Kconfig we merge:
      # CONFIG_DRM_MSM for the top-level `obj-$(CONFIG_DRM_MSM) += msm/`, and
      # CONFIG_ARCH_MONACO for msm/Kbuild's branch that includes
      # config/gki_monacodisp.conf and gki_monacodispconf.h. Without them no
      # .ko is built and the composer aborts in CreatePrimaryDisplay.
      #
      # Make variables, not a kernel fragment: CONFIG_DRM_MSM is a real
      # in-tree tristate, and in .config it would also build upstream msm.ko,
      # which binds the same qcom,sde-kms nodes.
      #
      # EXTRA_CFLAGS: mcu_display_exports.h is in the repo's include/ subdir,
      # not where msm/Kbuild's -I points.
      display)              echo "DISPLAY_ROOT=$P/display/ CONFIG_ARCH_MONACO=y CONFIG_DRM_MSM=y EXTRA_CFLAGS=-I${S}/private/google-modules/display/watch/mcu-offload/mcu_display/include" ;;
      # mcu_display: nanohub headers; Module.symvers chains via EXT_MODULES order.
      private/google-modules/display/watch/mcu-offload/mcu_display)
        echo "EXTRA_CFLAGS=-I${S}/private/google-modules/nanohub" ;;
      graphics)             echo "KGSL_MODULE_ROOT=$P/graphics/" ;;
      # bms{,/misc} and drivers/power/supply/qcom gate obj-* on CONFIG_GOOGLE_*
      # and CONFIG_QPNP_SMBLITE, which are in no Kconfig. EXT_MODULES order
      # bms/misc -> bms -> qcom -> usb_shim chains their Module.symvers.
      private/google-modules/bms/misc) echo "CONFIG_GOOGLE_VOTABLE=m CONFIG_GOOGLE_LOGBUFFER=m" ;;
      private/google-modules/bms)
        echo "CONFIG_GOOGLE_BMS=m CONFIG_GOOGLE_BATTERY=m CONFIG_GOOGLE_CHARGER=m CONFIG_GOOGLE_BMS_SW5100=m" ;;
      private/msm-google/drivers/power/supply/qcom)
        echo "CONFIG_QPNP_SMBLITE=m CONFIG_QTI_QBG=m" ;;
      # nanohub: invented CONFIG_NANOHUB* vars for the obj- lines. The matching
      # -D defines go in KMOD_MAKE's KCFLAGS, since modroot output is
      # shell-split and a multi-word KCFLAGS would be cut.
      private/google-modules/nanohub)
        echo "CONFIG_NANOHUB=m CONFIG_NANOHUB_SPI=y CONFIG_NANOHUB_BL_NXP=y CONFIG_NANOHUB_DISPLAY=y" ;;
      # nitrous: sol's Broadcom BT driver ("goog,nitrous", /soc/btbcm).
      # CONFIG_NITROUS=m comes from the bypassed wrapper Makefile. It links
      # logbuffer_* from bms/misc, which must stay ahead of it in EXT_MODULES.
      private/google-modules/bluetooth/broadcom)
        echo "CONFIG_NITROUS=m" ;;
      # syna dhd4381: sol's WLAN driver ("android,syna_wlan"). CONFIG_BCMDHD=m
      # turns the obj- line on (DRIVER_TYPE ?= $(CONFIG_BCMDHD)); the output is
      # synadhd4381.ko, as on stock. BCMDHD_ROOT comes from the bypassed wrapper.
      private/google-modules/wlan/syna/dhd4381)
        echo "CONFIG_BCMDHD=m BCMDHD_ROOT=${S}/private/google-modules/wlan/syna/dhd4381" ;;
      # mcu_mic_codec: nanohub headers, as above.
      private/google-modules/sound/mcu_mic_codec)
        echo "CONFIG_SND_SOC_MCU_MIC_CODEC=m EXTRA_CFLAGS=-I${S}/private/google-modules/nanohub" ;;
      # cs40l26: sol's haptic LRA driver (Cirrus CS40L26A, i2c 0x43), not a
      # speaker amp. Built by the repo's own Makefile.
      private/google-modules/amplifiers/cs40l26)
        echo "CONFIG_INPUT_CS40L26_I2C=m CONFIG_CIRRUS_FIRMWARE_CL_DSP=m CONFIG_SND_SOC_CS40L26=m" ;;
      *) echo "" ;;
    esac
}

do_compile() {
    # Without the kernel's Module.symvers modpost resolves no CRC, and with
    # KBUILD_MODPOST_WARN=1 every module ships an empty __versions; the KMI gate
    # in sol-boot-images then fails with a misleading CRC. A sstate-restored
    # kernel can leave it absent.
    [ -s "${KOUT}/Module.symvers" ] || bbfatal "${KOUT}/Module.symvers is missing: re-run virtual/kernel:do_shared_workdir (bitbake -f -c compile linux-sol) before building the modules"

    # sign-file (rebuilt by modules_prepare) needs openssl-native's headers and
    # libs. Set them compiler-wide: overriding HOST_EXTRACFLAGS breaks dtc's
    # libfdt include.
    export C_INCLUDE_PATH="${STAGING_INCDIR_NATIVE}${C_INCLUDE_PATH:+:$C_INCLUDE_PATH}"
    export LIBRARY_PATH="${STAGING_LIBDIR_NATIVE}${LIBRARY_PATH:+:$LIBRARY_PATH}"

    # meta-oe linux.inc does not prepare the kernel for M= builds
    # ("scripts/genksyms/genksyms: not found"); prepare it once.
    ${KMOD_MAKE} modules_prepare

    # Many Qualcomm Kbuilds include $(srctree)/../{msm-,}google-modules/...,
    # assuming the module trees are siblings of the kernel srctree.
    ln -sfn ${S}/private/msm-google-modules ${KDIR}/../msm-google-modules
    ln -sfn ${S}/private/google-modules     ${KDIR}/../google-modules
    # Expose the kernel as private/msm-google (the manifest layout) so
    # EXT_MODULES can name kernel-tree dirs like drivers/power/supply/qcom.
    ln -sfn ${KDIR} ${S}/private/msm-google

    # mcu-offload is bazel-only; give it a Kbuild before the M= loop.
    MCUD=${S}/private/google-modules/display/watch/mcu-offload/mcu_display
    if [ -d "$MCUD" ]; then
        install -m 0644 ${UNPACKDIR}/mcu_display-Kbuild "$MCUD/Kbuild"
    else
        bbfatal "mcu_display source missing at $MCUD (SRC_URI/destsuffix wrong?)"
    fi

    [ -f ${S}/private/google-modules/amplifiers/cs40l26/Makefile ] || \
        bbfatal "cs40l26 Makefile missing (SRC_URI/destsuffix or SRCREV_amplifiers wrong?)"

    extra=""
    for m in ${EXT_MODULES}; do
        rel="${m#private/msm-google-modules/}"
        bbnote "Building vendor module: $rel"
        if [ "$m" = "private/msm-google-modules/securemsm" ]; then
            # securemsm provides smcinvoke_dlkm + qseecom_dlkm (the TEE). Without
            # /dev/smcinvoke and /dev/qseecom the keymint HAL never registers,
            # keystore2 restart-storms ("Too many pending control messages") and
            # the container stalls before the composer starts.
            #
            # smcinvoke/compat/trace_smcinvoke.h hardcodes
            #   SMCINVOKE_TRACE_INCLUDE_PATH ../../../../../vendor/qcom/opensource/...
            # (the AOSP layout). It is #ifndef-guarded, so override it to "." and
            # add a matching -I; define_trace.h includes it with angle brackets.
            # Passed here, not via modroot(), because modroot output is
            # shell-split and a multi-word EXTRA_CFLAGS would be cut.
            ${KMOD_MAKE} M=${S}/$m $(modroot "$rel") \
                KBUILD_EXTRA_SYMBOLS="$extra" \
                EXTRA_CFLAGS="-DSMCINVOKE_TRACE_INCLUDE_PATH=. -I${S}/private/msm-google-modules/securemsm/smcinvoke/compat -I${S}/private/msm-google-modules/securemsm/include" modules
        else
            ${KMOD_MAKE} M=${S}/$m $(modroot "$rel") \
                KBUILD_EXTRA_SYMBOLS="$extra" modules
        fi
        sym="${S}/$m/Module.symvers"
        [ -f "$sym" ] && extra="$extra $sym"
    done

    # sol_vidc_stub: a do-nothing driver for 5a00000.qcom,vidc. msm_video is not
    # built, so that node is the one gcc-monaco consumer that never binds, gcc's
    # sync_state never runs, and every clock the bootloader left on (USB, eMMC,
    # two QUP engines, gpll0) stays on through suspend: 35 clocks at suspend
    # instead of 8. Drop it once msm_video is built.
    VS=${S}/sol-vidc-stub
    install -d $VS
    install -m 0644 ${UNPACKDIR}/sol_vidc_stub.c $VS/sol_vidc_stub.c
    install -m 0644 ${UNPACKDIR}/sol-vidc-stub-Kbuild $VS/Kbuild
    ${KMOD_MAKE} M=$VS modules
}

KREL ?= "${SOL_KERNEL_VERSION}"
# Collect every built .ko flat under the kernel-release dir; nonarch_base_libdir
# lands in /usr/lib under usrmerge.
do_install() {
    install -d ${D}${nonarch_base_libdir}/modules/${KREL}/vendor
    # Out-of-tree modules emit their .ko next to the sources.
    find ${S} -name '*.ko' -exec sh -c \
      'cp -n "$1" ${D}${nonarch_base_libdir}/modules/${KREL}/vendor/' _ {} \;
    # The kernel-tree modules (drivers/power/supply/qcom) build under ${KDIR},
    # reached through the private/msm-google symlink, which find does not
    # follow. `find -L` would also collect linux-sol's own modules, so take
    # that one directory explicitly.
    if [ -d ${KDIR}/drivers/power/supply/qcom ]; then
      find ${KDIR}/drivers/power/supply/qcom -maxdepth 1 -name '*.ko' \
        -exec cp -n {} ${D}${nonarch_base_libdir}/modules/${KREL}/vendor/ \;
    fi
}

FILES:${PN} = "${nonarch_base_libdir}/modules"
# arm64 .ko files in an armv7 machine recipe, on purpose. No debug-split.
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"
INSANE_SKIP:${PN} += "arch buildpaths"

# Same icecream defect as linux-sol: with clang, icecream compiles remotely and
# synthesises its own -MF instead of honouring -Wp,-MD,<path>, so fixdep sees no
# header deps and module rebuilds go stale.
ICECC_DISABLED = "1"
