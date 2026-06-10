SUMMARY = "Pixel Watch 2 (aurora) Qualcomm/Google vendor kernel modules"
DESCRIPTION = "Builds the sw5100 EXT_MODULES (audio/bt/dataipa/datarmnet/ \
display/graphics/mm/mmrm/securemsm/video/wlan...) from source against \
linux-aurora with the same clang + config, so vermagic/CRC/ABI match \
by construction (no KMI/AOSP-clang/prebuilt reuse). Repo set + revisions \
follow Google's kernel-platform repo manifest for this device \
(android.googlesource.com/kernel/manifest, branch \
android-msm-eos-5.15-tm-wear-kr3-dr-eos; google-modules/* pinned on the \
sibling -pixel-watch branch where noted)."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"
COMPATIBLE_MACHINE = "aurora"
# OE auto-appends "@PRSERV_PV_AUTOINC@+<every SRCREV concatenated>" to PKGV for
# a multi-SRCREV git recipe (package version-uniqueness), which is INDEPENDENT
# of PV/SRCPV — so the .ipk filename blew past the 255-byte NAME_MAX ("File
# name too long" in do_package_write_ipk) once the 18th repo (nvttouch) was
# added. Pin PKGV to a constant to stop the SRCREV-soup suffix. Source pinning
# is unaffected: the SRCREV_<name> lines pin every repo and bitbake task-hashes
# still track SRCREV changes; only the package version string is fixed.
require linux-aurora-version.inc
PV = "${AURORA_KERNEL_VERSION}"
PKGV = "${AURORA_KERNEL_VERSION}"

# Module repos laid out at the private/ paths build.config.sw5100 / the
# kernel-platform manifest expect (EXT_MODULES dirs live under
# private/msm-google-modules/, glue under private/google-modules/).
AGM = "git://android.googlesource.com/kernel"
SRC_URI = "\
  ${AGM}/msm-extra;protocol=https;branch=${KBR};name=audio;destsuffix=git/private/msm-google-modules/audio \
  ${AGM}/msm-modules/bt;protocol=https;branch=${KBR};name=bt;destsuffix=git/private/msm-google-modules/bt \
  ${AGM}/msm-extra/dataipa;protocol=https;branch=${KBR};name=dataipa;destsuffix=git/private/msm-google-modules/dataipa \
  ${AGM}/msm-modules/datarmnet;protocol=https;branch=${KBR};name=datarmnet;destsuffix=git/private/msm-google-modules/datarmnet \
  ${AGM}/msm-modules/datarmnet-ext;protocol=https;branch=${KBR};name=datarmnetext;destsuffix=git/private/msm-google-modules/datarmnet-ext \
  ${AGM}/msm-extra/display-drivers;protocol=https;branch=${KBR};name=display;destsuffix=git/private/msm-google-modules/display \
  ${AGM}/msm-modules/graphics;protocol=https;branch=${KBR};name=graphics;destsuffix=git/private/msm-google-modules/graphics \
  ${AGM}/msm-modules/mm;protocol=https;branch=${KBR};name=mm;destsuffix=git/private/msm-google-modules/mm \
  ${AGM}/msm-modules/mmrm;protocol=https;branch=${KBR};name=mmrm;destsuffix=git/private/msm-google-modules/mmrm \
  ${AGM}/msm-modules/securemsm;protocol=https;branch=${KBR};name=securemsm;destsuffix=git/private/msm-google-modules/securemsm \
  ${AGM}/msm-extra/video-driver;protocol=https;branch=${KBR};name=video;destsuffix=git/private/msm-google-modules/video \
  ${AGM}/msm-modules/wlan-platform;protocol=https;branch=${KBR};name=wlanplat;destsuffix=git/private/msm-google-modules/wlan/platform \
  ${AGM}/msm-modules/qcacld;protocol=https;branch=${KBR};name=qcacld;destsuffix=git/private/msm-google-modules/wlan/qcacld-3.0 \
  ${AGM}/msm-modules/qca-wfi-host-cmn;protocol=https;branch=${KBR};name=qcacmn;destsuffix=git/private/msm-google-modules/wlan/qca-wifi-host-cmn \
  ${AGM}/msm-modules/wlan-fw-api;protocol=https;branch=${KBR};name=wlanfwapi;destsuffix=git/private/msm-google-modules/wlan/fw-api \
  ${AGM}/google-modules/soc/msm;protocol=https;branch=android-msm-eos-5.15-tm-wear-kr3-pixel-watch;name=socmsm;destsuffix=git/private/google-modules/soc/msm \
  ${AGM}/google-modules/bms;protocol=https;branch=${KBR};name=bms;destsuffix=git/private/google-modules/bms \
  ${AGM}/google-modules/touch/novatek_touch;protocol=https;branch=${KBR};name=nvttouch;destsuffix=git/private/google-modules/touch/novatek \
  ${AGM}/google-modules/nanohub;protocol=https;branch=android-msm-eos-5.15-tm-wear-kr3-pixel-watch;name=nanohub;destsuffix=git/private/google-modules/nanohub \
  ${AGM}/google-modules/rotary-encoders;protocol=https;branch=android-msm-eos-5.15-tm-wear-kr3-pixel-watch;name=rotary;destsuffix=git/private/google-modules/rotary-encoders \
  ${AGM}/google-modules/sound/mcu_mic_codec;protocol=https;branch=android-msm-eos-5.15-tm-wear-kr3-pixel-watch;name=mcumic;destsuffix=git/private/google-modules/sound/mcu_mic_codec \
  ${AGM}/google-modules/amplifiers;protocol=https;branch=android-msm-eos-5.15-tm-wear-kr3-pixel-watch;name=amplifiers;destsuffix=git/private/google-modules/amplifiers \
  file://0001-audio-monaco-bind-haptics-codec-by-deterministic-MFD.patch \
  file://0002-icnss2-register-cnss-genl-family-for-ADRASTEA-too.patch \
  file://0003-bt-btpower-make-bt_clk_enable-idempotent.patch \
  file://0004-amplifiers-cs40l26-set-loaded-before-mfd-add-devices.patch \
  file://0005-amplifiers-cl_dsp-return-ENOENT-when-coeff-control-i.patch \
  file://0006-amplifiers-cs40l26-pin-codec-MFD-child-platform-id.patch \
"
KBR = "android-msm-eos-5.15-tm-wear-kr3-dr-eos"
SRCREV_audio       = "78c8eece9b0ee28d887176fe19b3dd77b80ffbdc"
SRCREV_bt          = "4abdd117b8ede17635959b2ac90a4b75d095368d"
SRCREV_dataipa     = "e6c725f81debef20fa8e0279ba46f576953d1e22"
SRCREV_datarmnet   = "b4a96e8a59d60f3189e2c9e545ed62f658c7823a"
SRCREV_datarmnetext = "10831c11185a84dfb56b5ebae968d1364277a2af"
SRCREV_display     = "4216bb955341b142b326a07a7a2d217450e4cb7f"
SRCREV_graphics    = "967881a258a5dccb137b209f204fe5709092dea0"
SRCREV_mm          = "03debc2f493a563eb2d7851bd5ceae46edb5194e"
SRCREV_mmrm        = "a197b8fe755b0e5521ee8aad6e405f8841ae06be"
SRCREV_securemsm   = "19299a41fbbcf3592508d01ecd3e0a6e13a20cb7"
SRCREV_nvttouch    = "ca758cbafad408e5120ecd77273325bd2e10e319"
SRCREV_video       = "ea6b16d14a7454c66e1089ef52350e96619e5216"
SRCREV_wlanplat    = "e117f3d6496cc3841b441a3864010e5286d45c79"
SRCREV_qcacld      = "287468e01cc187c230a1843829cb22b55e2e042b"
SRCREV_qcacmn      = "5e54d8c3174d8e785914cfaefe2f1fc7aa04ae61"
SRCREV_wlanfwapi   = "a9859f1dca58c88bb4887f580bf704b2242ce602"
# socmsm is pinned to the PIXEL-WATCH branch (aurora variant) — its
# build.config.sw5100 EXT_MODULES authoritatively includes 'usb_shim' (the
# google,extcon-usb-shim driver our DT requires). The dr-eos branch is a strict
# subset (LTE variant; lacks usb_shim). See SRC_URI override.
SRCREV_socmsm      = "7a2cd1649e49373ca24bd81d4fe89e7440db4ab5"
SRCREV_bms         = "51583026f264e7597808a16aa6809fb9279eb8c4"
# nanohub (sensor-hub MCU driver): Google-specific module pinned to the same
# pixel-watch branch as socmsm. Single-dir Kbuild; CONFIG_NANOHUB_* vars aren't
# in any Kconfig (invented for this tree) so we pass them via modroot + the
# corresponding `-DCONFIG_NANOHUB_*` flags via KMOD_MAKE's KCFLAGS (the source
# uses #ifdef gates on these names).
SRCREV_nanohub     = "58eca049cf800fee3f741fa34397f40dac7cb35d"
# rotary-encoders ships petc_input_filter (input event filter that hooks into
# nanohub via nanohub_register_listener) + ots_pat9126 (optical touch sensor);
# we build petc_input_filter so the MCU's input-channel messages have a
# kernel-side consumer -- without it the MCU may sit in pre-handshake state and
# mcu_mgmtd retries the firmware-download cycle.
SRCREV_rotary      = "73dc43decfde15cf3fd1623b39a5f8f4dfec4fc9"
# sound/mcu_mic_codec is the ASoC codec that consumes nanohub's audio channel.
# Same role as petc_input_filter on a different nanohub_io channel.
SRCREV_mcumic      = "94deec7a3e641a845ebe7468341e902d0d2304fa"
SRCREV_amplifiers  = "06552df7596558c5b9b0fe1ad39fefc4ae116ef7"
SRCREV_FORMAT = "audio_bt_dataipa_datarmnet_datarmnetext_display_graphics_mm_mmrm_securemsm_video_wlanplat_qcacld_qcacmn_wlanfwapi_socmsm_bms_nvttouch_nanohub_rotary_mcumic_amplifiers"

S = "${WORKDIR}/git"

# Build against the exact linux-aurora kernel (same clang + config → matching
# vermagic/CRC/ABI). Use its shared build artifacts.  Host tools the kernel's
# modules_prepare/prepare0 needs (meta-oe linux.inc built these for the kernel;
# this recipe needs them too).
DEPENDS += "virtual/kernel clang-native"
DEPENDS += "bc-native bison-native flex-native openssl-native kmod-native rsync-native coreutils-native"
do_configure[depends] += "virtual/kernel:do_shared_workdir"

LLVM_BIN = "${STAGING_BINDIR_NATIVE}"
PATH:prepend = "${STAGING_BINDIR_NATIVE}:"
# linux-aurora uilds out-of-tree (B != S): kernel SOURCE in STAGING_KERNEL_DIR,
# built artifacts (.config/Module.symvers/genksyms) in STAGING_KERNEL_BUILDDIR.
# Canonical OE external-module build: make -C <src> O=<builddir> M=...
KDIR = "${STAGING_KERNEL_DIR}"
KOUT = "${STAGING_KERNEL_BUILDDIR}"

# EXT_MODULES build order from build.config.sw5100 (M= dirs relative to S).
EXT_MODULES = "\
  private/msm-google-modules/mm/sync_fence \
  private/msm-google-modules/mm/hw_fence \
  private/msm-google-modules/mm/msm_ext_display \
  private/msm-google-modules/mmrm \
  private/msm-google-modules/securemsm \
  private/msm-google-modules/audio \
  private/msm-google-modules/display \
  private/msm-google-modules/graphics \
  private/msm-google-modules/video \
  private/msm-google-modules/bt \
  private/msm-google-modules/dataipa/drivers/platform/msm \
  private/msm-google-modules/datarmnet/core \
  private/msm-google-modules/datarmnet-ext/wlan \
  private/msm-google-modules/wlan/platform \
  private/google-modules/touch/novatek/nt38350 \
  private/msm-google-modules/wlan/qcacld-3.0 \
  private/google-modules/bms/misc \
  private/google-modules/bms \
  private/msm-google/drivers/power/supply/qcom \
  private/google-modules/soc/msm/usb_shim \
  private/google-modules/soc/msm/wlan_mac \
  private/google-modules/nanohub \
  private/google-modules/rotary-encoders/petc_input_filter \
  private/google-modules/sound/mcu_mic_codec \
  private/google-modules/amplifiers/cs40l26 \
"
# qcacld-3.0 is the WiFi driver. Building it out-of-tree (via M=) against this
# kernel needs four accommodations, all applied here / in linux-aurora:
#  - Two linux-aurora kbuild patches (cmd-mod-file-func.patch,
#    cc-o-c-respfile.patch): qcacld's per-module object list and per-file
#    compile flags each exceed the kernel MAX_ARG_STRLEN single-argv limit; the
#    patches route them through a GNU make $(file ...) / clang @response-file
#    instead. Generic and harmless for every module.
#  - The full qcacld KBUILD_OPTIONS replicated in modroot() (qcacld's own
#    Makefile only sets them when WLAN_ROOT is unset; an M= build sets
#    WLAN_ROOT and bypasses that), plus a broad qca-wifi-host-cmn header path
#    set: the 'wear' profile feature-gates DFS/spectral but qcacmn's public
#    headers #include them unconditionally.
#  - WLAN_COMMON_ROOT passed as the relative `cmn` symlink (qcacld builds
#    object paths as $(WLAN_ROOT)/$(WLAN_COMMON_ROOT)/...; an absolute value
#    doubles the path).
#  - -DCFG80211_SINGLE_NETDEV_MULTI_LINK_SUPPORT: the google-eos kernel
#    backported the 4-arg MLO cfg80211_ch_switch_notify, which qcacld cannot
#    auto-detect, so it must be told to use the 4-arg path.

KMOD_MAKE = "make -C ${KDIR} O=${KOUT} ARCH=arm64 LLVM=1 LLVM_IAS=1 \
  CROSS_COMPILE=aarch64-linux-gnu- \
  CC='${LLVM_BIN}/clang --target=aarch64-linux-gnu -fuse-ld=lld' \
  LD='${LLVM_BIN}/ld.lld' AR='${LLVM_BIN}/llvm-ar' \
  NM='${LLVM_BIN}/llvm-nm' OBJCOPY='${LLVM_BIN}/llvm-objcopy' \
  KERNEL_SRC=${KDIR} OUT_DIR=${KDIR} \
  KCFLAGS='-Wno-error -Wno-error=implicit-int -Wno-error=int-conversion -Wno-error=incompatible-function-pointer-types -Wno-error=incompatible-pointer-types -Wno-error=implicit-function-declaration -Wno-error=strict-prototypes -I${KDIR}/drivers/clk/qcom -I${KDIR}/drivers/devfreq -I${KDIR}/include/linux -I${KDIR}/drivers/iommu -I${S}/private/msm-google-modules/wlan/qca-wifi-host-cmn/utils/sys -DCONFIG_NANOHUB -DCONFIG_NANOHUB_SPI -DCONFIG_NANOHUB_BL_NXP -DCONFIG_NANOHUB_DISPLAY' \
  KBUILD_MODPOST_WARN=1 CONFIG_ARCH_MONACO=y WLAN_PROFILE=wear \
  CONFIG_CNSS_QCA6750=m CONFIG_QCA_CLD_WLAN=m CONFIG_TOUCHSCREEN_NT38350=m"
# KERNEL_SRC / OUT_DIR: google-modules/bms/Makefile and
# msm-google/drivers/power/supply/qcom/Makefile are "wrapper" Makefiles (not
# just Kbuild fragments). They `include $(KERNEL_SRC)/../google-modules/
# soc/msm/Makefile.include` and reference $(OUT_DIR)/../google-modules/bms/
# misc/Module.symvers for KBUILD_EXTRA_SYMBOLS chaining. Both default to
# `/lib/modules/$(uname -r)/build` (HOST kernel) — wrong for OE cross builds,
# breaks the include. Point both at ${KDIR}: its parent (work-shared) already
# holds our `google-modules` + `msm-google-modules` symlinks (created in
# do_compile), so `$(KERNEL_SRC)/../google-modules/...` and
# `$(OUT_DIR)/../google-modules/...` resolve to our trees.

# do_package debug-split/strip uses recipe binutils; machine is armv7 →
# defaults choke on the aarch64 .ko. Use clang/llvm-* + don't strip.
OBJCOPY = "${LLVM_BIN}/llvm-objcopy"
STRIP = "${LLVM_BIN}/llvm-strip"
INHIBIT_SYSROOT_STRIP = "1"

do_configure[noexec] = "1"

# Each Qualcomm module Kbuild expects a <NAME>_ROOT make var pointing at its
# *repo* root (Kbuild does e.g. include $(SYNC_FENCE_ROOT)/config/...); the
# kernel-platform build passes these. Map module dir -> "VAR=reporoot".  NOTE:
# Qualcomm Kbuilds concatenate WITHOUT a separator, e.g.
# `-I$(SYNC_FENCE_ROOT)sync_fence/include/`, so every *_ROOT value MUST end
# with a trailing slash.
modroot() {
    P="${S}/private/msm-google-modules"
    case "$1" in
      mm/sync_fence)        echo "SYNC_FENCE_ROOT=$P/mm/" ;;
      mm/hw_fence)          echo "MSM_HW_FENCE_ROOT=$P/mm/" ;;
      mm/msm_ext_display)   echo "MSM_EXT_DISPLAY_ROOT=$P/mm/" ;;
      mmrm)                 echo "MMRM_ROOT=$P/mmrm/" ;;
      securemsm)            echo "SSG_MODULE_ROOT=$P/securemsm/" ;;
      # Audio Kbuild gates the Monaco/sw5100 build on TWO things: MODNAME
      # non-empty (sets internal KERNEL_BUILD=0 -- the Android- style
      # external-build path) AND CONFIG_ARCH_MONACO=y (matches the
      # specific platform branch). Both together cause the Kbuild to `include
      # $(AUDIO_ROOT)/config/sw5100auto.conf` which exports
      # CONFIG_MSM_ADSP_LOADER=m, CONFIG_SPF_CORE=m, CONFIG_AUDIO_PKT=m,
      # CONFIG_MSM_QDSP6_NOTIFIER=m, CONFIG_MSM_QDSP6_PDR=m, etc., and
      # -includes config/sw5100autoconf.h for the matching #define CONFIG_*.
      # Without both, no audio .ko's compile — and adsp_loader_dlkm in
      # particular is required to create /sys/kernel/boot_adsp/boot, the sysfs
      # that triggers ADSP subsystem boot. ADSP boot is in turn a prerequisite
      # for the modem subsystem (modem fw waits for ADSP via tmr_slave2;
      # without ADSP the modem watchdog stalls at "DOG detects stalled
      # initialization" and recursively crashes).
      audio)                echo "AUDIO_ROOT=$P/audio/ MODNAME=audio CONFIG_ARCH_MONACO=y" ;;
      display)              echo "DISPLAY_ROOT=$P/display/" ;;
      graphics)             echo "KGSL_MODULE_ROOT=$P/graphics/" ;;
      video)                echo "VIDEO_ROOT=$P/video/" ;;
      # bt techpack: top-level Kbuild descends into slimbus/ + pwr/ ONLY when
      # CONFIG_BTFM_SLIM=m and CONFIG_MSM_BT_POWER=m are passed (see
      # bt/Kbuild). Without these, kbuild visits bt/ but produces nothing.
      # bt_fm_slim.ko registers the "btfmslim_slave" slim device that
      # monaco-asoc-snd's WCN BT dai_links (qcom,wcn-btfm=1) reference --
      # without it, the sound card defers forever.
      bt)                   echo "BT_ROOT=$P/bt/ CONFIG_BTFM_SLIM=m CONFIG_MSM_BT_POWER=m" ;;
      dataipa/*)            echo "IPA_ROOT=$P/dataipa/" ;;
      datarmnet/*)          echo "RMNET_CORE_ROOT=$P/datarmnet/" ;;
      datarmnet-ext/*)      echo "RMNET_EXT_ROOT=$P/datarmnet-ext/ RMNET_CORE_INC_DIR=$P/datarmnet/core RMNET_CORE_ROOT=$P/datarmnet/" ;;
      # wlan/platform = CNSS WLAN PLATFORM = the kernel-side broker qcacld-3.0
      # calls into via icnss_* / cnss_utils_* / cnss_nl_* symbols (QMI to FW,
      # firmware load, SMP2P, mem preallocator, generic netlink). Its top
      # Kbuild has `obj-$(CONFIG_ICNSS2) += icnss2/` etc., gated on CONFIG_*=m
      # which AREN'T in any Kconfig; the wrapper Makefile auto-sets them only
      # when WLAN_PLATFORM_ROOT is empty (`ifeq($(WLAN_PLATFORM_ROOT),)`) — and
      # we MUST pass WLAN_PLATFORM_ROOT (subdir Kbuilds do
      # `-I$(WLAN_PLATFORM_ROOT)/inc`), so the auto-config block is skipped.
      # Pass the CONFIG_*=m set ourselves. Skip CONFIG_CNSS2 (PCIe variant, n/a
      # for aurora which is integrated/ICNSS).  CONFIG_CNSS_OUT_OF_TREE=y
      # switches subdir -I paths from
      # $(srctree)/drivers/net/wireless/cnss_utils (Google addition, absent in
      # our kernel tree) to $(WLAN_PLATFORM_ROOT)/inc.
      # CONFIG_ICNSS2_DEBUG deliberately NOT set: its only effect is turning
      # ICNSS_ASSERT() from a no-op into BUG() (icnss2/debug.h). The asserts
      # fire on conditions that are recoverable-by-design — e.g. a WLFW
      # server-arrive while already connected (main.c
      # icnss_driver_event_server_arrive) or a failed BDF download
      # (qmi.c ICNSS_QMI_ASSERT) — turning a loggable error into a kernel
      # panic and, on a watch with no console, a reboot loop. The wrapper
      # Makefile itself only enables it in its everything-on developer
      # default block (which we bypass by passing WLAN_PLATFORM_ROOT).
      wlan/platform)        echo "WLAN_PLATFORM_ROOT=$P/wlan/platform/ CONFIG_CNSS_OUT_OF_TREE=y CONFIG_ICNSS2=m CONFIG_ICNSS2_QMI=y CONFIG_CNSS_UTILS=m CONFIG_CNSS_GENL=m CONFIG_WCNSS_MEM_PRE_ALLOC=m CONFIG_CNSS_QMI_SVC=m CONFIG_CNSS_PLAT_IPC_QMI_SVC=m" ;;
      # qcacld's own Makefile sets a full KBUILD_OPTIONS set ONLY when it
      # computes WLAN_ROOT (ifeq $(WLAN_ROOT),) — and we both pass WLAN_ROOT
      # and bypass that Makefile (generic M= loop, needed for our out-of-tree
      # O=). qcacld's Kbuild gates its per-subsystem -I dirs on exactly these
      # CONFIG_*; not setting them => cascading 'file not found' (queue.h,
      # wlan_dfs_ioctl.h, ...). So pass the wrapper's full option set
      # ourselves.  WLAN_COMMON_ROOT must be RELATIVE (qcacld builds object
      # paths as $(WLAN_ROOT)/$(WLAN_COMMON_ROOT)/...; absolute => doubled path
      # "No rule to make target"). qcacld-3.0 ships the symlink cmn ->
      # ../qca-wifi-host-cmn (Kbuild/Android.mk default WLAN_COMMON_ROOT=cmn);
      # use it. WLAN_COMMON_INC = the abs path through that symlink.
      # MODNAME=wlan is intentional: any other name triggers Kbuild line
      # 4430-4433 to define MULTI_IF_NAME=$(MODNAME), which makes the driver's
      # wlan_hdd_misc.h prefix WLAN_INI_FILE/WLAN_MAC_FILE with an extra
      # "$(MODNAME)/" path segment -- breaking firmware lookup because
      # /vendor/firmware/wlan/qca_cld/WCNSS_qcom_cfg.ini actually lives at that
      # exact path (no extra subdir). The .ko produced is "wlan.ko".
      # PANIC_ON_BUG=n WLAN_WARN_ON_ASSERT=n: WLAN_PROFILE=wear includes
      # configs/wear_defconfig, which force-enables both whenever the kernel
      # has CONFIG_SLUB_DEBUG=y (ours does — GKI default). With PANIC_ON_BUG
      # defined, every QDF_BUG()/QDF_DEBUG_PANIC() in qcacld/qca-wifi-host-cmn
      # calls BUG() (cmn qdf/linux/src/i_qdf_trace.h); without it they compile
      # to logged no-ops — the same asserts-off mode QC ships on perf builds.
      # A failed assert then costs us a wifi error message instead of a
      # PS_HOLD reboot loop (the 2026-06-06 cnss-daemon incident). Make
      # command-line vars override the defconfig's `:=` assignments, so
      # passing them here wins. CONFIG_ICNSS2_DEBUG dropped from this case
      # too: qcacld's Kbuild never references it (it was inert copy-paste
      # from the wlan/platform option set).
      wlan/qcacld-3.0)      echo "WLAN_ROOT=$P/wlan/qcacld-3.0/ WLAN_COMMON_ROOT=cmn WLAN_COMMON_INC=$P/wlan/qcacld-3.0/cmn WLAN_FW_API=$P/wlan/fw-api/ MODNAME=wlan WLAN_WEAR_CHIPSET=qca_cld3 CONFIG_QCA_CLD_WLAN=m CONFIG_QCA_WIFI_ISOC=0 CONFIG_QCA_WIFI_2_0=1 WLAN_CTRL_NAME=wlan WLAN_PROFILE=wear PANIC_ON_BUG=n WLAN_WARN_ON_ASSERT=n CONFIG_CNSS_OUT_OF_TREE=y CONFIG_ICNSS2=m CONFIG_ICNSS2_QMI=y CONFIG_CNSS_GENL=m CONFIG_WCNSS_MEM_PRE_ALLOC=m CONFIG_CNSS_UTILS=m CONFIG_CONNECTIVITY_PKTLOG=y KERNEL_SUPPORTS_NESTED_COMPOSITES=n" ;;
      # google-modules/bms{,/misc} & msm-google/drivers/power/supply/qcom are
      # out-of-tree Kbuilds that gate their obj-* on CONFIG_GOOGLE_* /
      # CONFIG_QPNP_SMBLITE which are NOT in any Kconfig (they're invented for
      # these external trees). Pass them as make vars so obj-$(CONFIG_*)
      # selects the right composites. Order in EXT_MODULES is bms/misc -> bms
      # -> qcom -> usb_shim so each successor sees the predecessors'
      # Module.symvers.
      private/google-modules/bms/misc) echo "CONFIG_GOOGLE_VOTABLE=m CONFIG_GOOGLE_LOGBUFFER=m" ;;
      private/google-modules/bms)
        echo "CONFIG_GOOGLE_BMS=m CONFIG_GOOGLE_BATTERY=m CONFIG_GOOGLE_CHARGER=m CONFIG_GOOGLE_BMS_SW5100=m" ;;
      private/msm-google/drivers/power/supply/qcom)
        echo "CONFIG_QPNP_SMBLITE=m CONFIG_QTI_QBG=m" ;;
      # google-modules/soc/msm/wlan_mac has a Kbuild with a buggy relative -I
      # path ("../../../../private/msm-google-modules/wlan/platform/inc") --
      # one level too deep, resolves to ../private/
      # private/msm-google-modules/... which doesn't exist. The kernel-
      # platform build doesn't hit this because its source tree layout is
      # different. Inject the correct absolute include via EXTRA_CFLAGS (kbuild
      # appends it to ccflags-y) -- the existing broken -I path remains in
      # ccflags but is silently ignored.
      private/google-modules/soc/msm/wlan_mac)
        echo "EXTRA_CFLAGS=-I${S}/private/msm-google-modules/wlan/platform/inc" ;;
      # nanohub's Kbuild gates compilation on CONFIG_NANOHUB and
      # CONFIG_NANOHUB_{SPI,BL_NXP,DISPLAY} (not in any Kconfig -- invented
      # vars). Pass them so obj-* lines resolve. The matching
      # `-DCONFIG_NANOHUB*` source #ifdef gates are armed via the global
      # KMOD_MAKE KCFLAGS additions; cannot pass them here because modroot
      # output is shell-split and KCFLAGS with embedded spaces survive only as
      # KCFLAGS=<first-word>.
      private/google-modules/nanohub)
        echo "CONFIG_NANOHUB=m CONFIG_NANOHUB_SPI=y CONFIG_NANOHUB_BL_NXP=y CONFIG_NANOHUB_DISPLAY=y" ;;
      # petc_input_filter (rotary-encoders/petc_input_filter): the wrapper
      # Makefile sets EXTRA_INCLUDE=-I$(KERNEL_SRC)/../google-modules/nanohub
      # and KBUILD_EXTRA_SYMBOLS to nanohub's Module.symvers. We bypass the
      # wrapper (using kernel's M= build directly) so we pass the include path
      # via EXTRA_CFLAGS (same trick as wlan_mac). Module.symvers chaining
      # happens via our do_compile loop's $extra accumulator (which includes
      # nanohub's Module.symvers since nanohub is earlier in EXT_MODULES).
      private/google-modules/rotary-encoders/petc_input_filter)
        echo "CONFIG_INPUT_PETC_INPUT_FILTER=m EXTRA_CFLAGS=-I${S}/private/google-modules/nanohub" ;;
      # mcu_mic_codec uses nanohub.h headers via $(KERNEL_SRC)/../google-modules/nanohub.
      private/google-modules/sound/mcu_mic_codec)
        echo "CONFIG_SND_SOC_MCU_MIC_CODEC=m EXTRA_CFLAGS=-I${S}/private/google-modules/nanohub" ;;
      # cs40l26 (CS40L26 haptics + boosted-speaker amp on i2c@0-0043).  The
      # module's Makefile auto-adds the -DCONFIG_INPUT_CS40L26_ATTR_UNDER_BUS
      # + -DCONFIG_GOOG_CUST defines via its own EXTRA_CFLAGS += lines, so we
      # only pass the CONFIG_* tristate vars here.
      private/google-modules/amplifiers/cs40l26)
        echo "CONFIG_INPUT_CS40L26_I2C=m CONFIG_CIRRUS_FIRMWARE_CL_DSP=m CONFIG_SND_SOC_CS40L26=m" ;;
      *) echo "" ;;
    esac
}

do_compile() {
    # qcacld (wifi) is enormous; its wlan.mod rule expands thousands of object
    # paths into one argv -> "Argument list too long". Raise the stack limit
    # (ARG_MAX scales with it) — the standard qcacld out-of-tree build fix.
    ulimit -s unlimited 2>/dev/null || ulimit -s 131072 2>/dev/null || true

    # meta-oe linux.inc shares an in-tree-built kernel-source but doesn't
    # populate STAGING_KERNEL_BUILDDIR like kernel.bbclass; external module
    # builds then trip "scripts/genksyms/genksyms: not found" because the
    # kernel isn't prepared for M= builds. Prepare it once (writable
    # work-shared dir) so scripts/modpost/genksyms state is consistent.
    ${KMOD_MAKE} modules_prepare

    # Many Qualcomm Kbuilds use $(srctree)/../msm-google-modules/... and
    # $(srctree)/../google-modules/... relative includes — they assume the
    # kernel_platform layout where the module trees are SIBLINGS of the kernel
    # srctree. Recreate that with symlinks next to STAGING_KERNEL_DIR so those
    # relative paths resolve.
    ln -sfn ${S}/private/msm-google-modules ${KDIR}/../msm-google-modules
    ln -sfn ${S}/private/google-modules     ${KDIR}/../google-modules
    # Make the kernel source addressable from the modules tree as
    # `private/msm-google` (Google's manifest layout). Lets EXT_MODULES
    # reference kernel-tree subdirs like drivers/power/supply/qcom for
    # out-of-tree builds — qpnp-smblite-main.ko etc.
    ln -sfn ${KDIR} ${S}/private/msm-google

    # qcacld 'wear' profile disables DFS/spectral/etc. but qcacmn public
    # headers #include them unconditionally -> the Kbuild's feature-gated -I
    # dirs are absent -> cascading 'file not found' (queue.h in utils/sys,
    # wlan_dfs_ioctl.h in umac/dfs/.../inc, ...). Fix: add EVERY header dir
    # under the wlan trees to -I. This is huge but cc-o-c-respfile.patch routes
    # $(c_flags) through an @response-file so it no longer overflows the cc
    # command. Computed at build time.
    WL=${S}/private/msm-google-modules/wlan
    WINC=$(for d in "$WL/qca-wifi-host-cmn" "$WL/qcacld-3.0" "$WL/fw-api" "$WL/platform/inc"; do \
            find "$d" -name '*.h' -printf '%h\n' 2>/dev/null; done | sort -u | sed 's/^/-I/' | tr '\n' ' ')

    extra=""
    for m in ${EXT_MODULES}; do
        rel="${m#private/msm-google-modules/}"
        bbnote "Building vendor module: $rel"
        if [ "$m" = "private/msm-google-modules/wlan/qcacld-3.0" ]; then
            ${KMOD_MAKE} M=${S}/$m $(modroot "$rel") \
                KBUILD_EXTRA_SYMBOLS="$extra" \
                KCFLAGS="-Wno-error -Wno-implicit-function-declaration -Wno-error=implicit-int -Wno-error=int-conversion -Wno-error=incompatible-function-pointer-types -Wno-error=incompatible-pointer-types -Wno-error=implicit-function-declaration -Wno-error=strict-prototypes -DCFG80211_SINGLE_NETDEV_MULTI_LINK_SUPPORT $WINC" modules
        else
            ${KMOD_MAKE} M=${S}/$m $(modroot "$rel") \
                KBUILD_EXTRA_SYMBOLS="$extra" modules
        fi
        sym="${S}/$m/Module.symvers"
        [ -f "$sym" ] && extra="$extra $sym"
    done
}

KREL ?= "${AURORA_KERNEL_VERSION}"
# Collect every built techpack .ko flat under the kernel-release dir. Use
# ${nonarch_base_libdir} so it lands in /usr/lib under the usrmerge distro
# feature.
do_install() {
    install -d ${D}${nonarch_base_libdir}/modules/${KREL}/vendor
    # The bulk of EXT_MODULES (out-of-tree under ${S}/private/...) emit .ko's
    # next to their sources — find ${S} catches them.
    find ${S} -name '*.ko' -exec sh -c \
      'cp -n "$1" ${D}${nonarch_base_libdir}/modules/${KREL}/vendor/' _ {} \;
    # The kernel-tree EXT_MODULES (drivers/power/supply/qcom --
    # qpnp-smblite-main.ko, qti-qbg-main.ko) build their .ko's in
    # ${KDIR}/drivers/... reachable via the ${S}/private/msm-google
    # symlink, but `find ${S}` doesn't traverse symlinks (and `find -L` would
    # also drag in every linux-aurora-built kernel module — name conflict with
    # the kernel package). Collect those kernel-tree EXT modules by explicit
    # subdir to stay scoped.
    if [ -d ${KDIR}/drivers/power/supply/qcom ]; then
      find ${KDIR}/drivers/power/supply/qcom -maxdepth 1 -name '*.ko' \
        -exec cp -n {} ${D}${nonarch_base_libdir}/modules/${KREL}/vendor/ \;
    fi
}

FILES:${PN} = "${nonarch_base_libdir}/modules"
# aarch64 .ko on an armv7 machine recipe; these are intentionally arm64. No
# debug-split (kills the .debug arch QA + spurious -dbg package).
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"
INSANE_SKIP:${PN} += "arch buildpaths"
