SUMMARY = "Dace flashable boot artifacts: boot.img, vendor_kernel_boot.img, init_boot.img"
DESCRIPTION = "\
Builds the three .img files dace's bootloader expects, end-to-end from \
bitbake-built inputs: \
\
 - boot.img            = our linux-dace kernel Image, empty ramdisk, \
                         mkbootimg header v4 \
 - vendor_kernel_boot  = ramdisk (kernel modules per dace-vkb-modules.lst, \
                         KMI-CRC-gated against this kernel) + DTB with \
                         ramoops node injected, mkbootimg header v4 \
 - init_boot.img       = initramfs-android-image's cpio.gz, repacked as \
                         lz4, mkbootimg header v4 \
\
The base DTB is shipped as a static input (vkb-base.dtb) -- the kernel \
doesn't build dtbs from gki_defconfig, and dace's runtime DT is the \
bootloader-assembled one anyway."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# The DTBs come from ota-stock/blobs: monaco-real.dtb + monacop.dtb are the
# ones the T5 ABL expects (T5 board-id). vkb-base.dtb (aurora) was rejected
# by the ABL (EDL 05c6:900e). monaco-real already carries ramoops@9ff00000 and splash.
SRC_URI = "\
    file://dace-vkb-assemble.py \
    file://dace-vkb-modules.lst \
    file://static/modules.alias \
    file://static/modules.softdep \
    file://static/modules.load.charger \
    file://static/modules.dep \
    file://static/bootconfig \
    file://static/monaco-real.dtb \
    file://static/monacop.dtb \
    file://static/monaco-idp-v1-overlay.dtbo \
    file://static/stock/qti-qbg-main.ko \
    file://static/stock/power_state.ko \
"
S = "${UNPACKDIR}"

COMPATIBLE_MACHINE = "dace"

# DEPENDS:
#  - virtual/kernel: provides Image + Module.symvers + kernel-built .ko's
#                    (we read them out of the linux-dace workdir).
#  - linux-dace-modules: techpack .ko's; we read its deploy ipk.
#  - initramfs-android-image: init_boot.img ramdisk content (cpio.gz).
#  - mkbootimg-tools-native: provides ${STAGING_BINDIR_NATIVE}/mkbootimg
#  - dtc-native: provides ${STAGING_BINDIR_NATIVE}/fdtput
DEPENDS = "\
    virtual/kernel \
    linux-dace-modules \
    initramfs-android-image \
    mkbootimg-tools-native \
    dtc-native \
    clang-native \
"
do_compile[depends] += "\
    virtual/kernel:do_compile \
    virtual/kernel:do_install \
    linux-dace-modules:do_package_write_ipk \
    initramfs-android-image:do_image_complete \
"

PACKAGES = ""
inherit deploy nopackages

# ─── Kernel artifact paths ───
# linux-dace builds out-of-tree: source in STAGING_KERNEL_DIR,
# .config/Image/Module.symvers in STAGING_KERNEL_BUILDDIR. .ko's install into
# the recipe's package/ via kernel.bbclass.
# layer.conf appends ${LAYERDIR} to BBPATH, so this layer-root-relative
# require resolves from any recipe in meta-dace.
require recipes-kernel/linux/linux-dace-version.inc
KMODVER ?= "${DACE_KERNEL_VERSION}"
# Kernel KREL: DACE_KERNEL_VERSION + EXTRAVERSION of the vendorkernel
# (5.15.144 + -g7f9d6c16b5cd-ab151). The kernel workdir's /lib/modules/<KREL>
# folder uses this name, not the bare DACE_KERNEL_VERSION.
DACE_KREL ?= "${DACE_KERNEL_VERSION}-g7f9d6c16b5cd-ab151"
# linux-dace's workdir uses ${MACHINE}${TARGET_VENDOR}-${TARGET_OS}
# (= dace-oe-linux-gnueabi). do_install drops .ko's into package/.
LINUX_DACE_WORKDIR ?= "${TMPDIR}/work/${MACHINE}${TARGET_VENDOR}-${TARGET_OS}/linux-dace/${KMODVER}+git"
LINUX_DACE_PKGDIR  ?= "${LINUX_DACE_WORKDIR}/package/usr/lib/modules/${DACE_KREL}/kernel"
# linux-dace keeps its build artifacts in its own workdir's build/
# (out-of-tree B != S). STAGING_KERNEL_BUILDDIR is empty because we don't go
# through the standard kernel.bbclass staging for these.
LINUX_DACE_KIMG    ?= "${LINUX_DACE_WORKDIR}/build/arch/arm64/boot/Image"
LINUX_DACE_SYMVERS ?= "${LINUX_DACE_WORKDIR}/build/Module.symvers"

# Techpack ipk path. armv7vehf-neon is dace's userspace PKGARCH.
LINUX_DACE_MODULES_IPK ?= "${DEPLOY_DIR_IPK}/armv7vehf-neon/linux-dace-modules_${KMODVER}-r0_armv7vehf-neon.ipk"

# ─── mkbootimg v4 geometry ───
# BASE 0: the T5 ABL expects SMALL ABSOLUTE load addresses
# (kernel_load_addr=0x8000, ramdisk=0x1000000, dtb=0x1f00000, tags=0x100),
# NOT base+offset (0x10000000+0x8000=0x10008000). With base!=0 the vendor_boot
# is rejected -> direct EDL 05c6:900e. Same as the ticwatch batches that used
# to boot ("base 0, NOT 0x10000000").
MKBOOTIMG_BASE          ?= "0x0"
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

    # ─── Step 1: extract techpack ipk so dace-vkb-assemble.py can find .ko's ───
    if [ ! -f "${LINUX_DACE_MODULES_IPK}" ]; then
        bbfatal "linux-dace-modules ipk not at ${LINUX_DACE_MODULES_IPK} -- check linux-dace-modules:do_deploy"
    fi
    rm -rf ${WORKDIR}/tpmod
    mkdir -p ${WORKDIR}/tpmod
    ( cd ${WORKDIR}/tpmod && ar x "${LINUX_DACE_MODULES_IPK}" && tar -xf data.tar.* )

    # ─── Step 2: assemble ramdisk dir via the python helper ───
    if [ ! -d "${LINUX_DACE_PKGDIR}" ]; then
        bbfatal "linux-dace kernel modules not at ${LINUX_DACE_PKGDIR} -- check virtual/kernel build"
    fi
    if [ ! -f "${LINUX_DACE_SYMVERS}" ]; then
        bbfatal "Module.symvers not at ${LINUX_DACE_SYMVERS}"
    fi
    rm -rf ${WORKDIR}/vkb_ramdisk
    python3 ${S}/dace-vkb-assemble.py \
        --manifest      ${S}/dace-vkb-modules.lst \
        --kernel-pkgdir "${LINUX_DACE_PKGDIR}" \
        --techpack-dir  ${WORKDIR}/tpmod \
        --stock-dir     ${S}/static/stock \
        --symvers       "${LINUX_DACE_SYMVERS}" \
        --static-dir    ${S}/static \
        --out           ${WORKDIR}/vkb_ramdisk

    # ─── Step 2.5: strip debug info from aarch64 kernel modules ───
    # This recipe runs in an armv7 context but the vendor kernel modules
    # are aarch64 ELF. The default strip tool silently does nothing on a
    # foreign ELF target, leaving full debug info in every .ko file.
    # llvm-strip from clang-native is architecture-agnostic and correctly
    # strips aarch64 ELF from any build context.
    LLVM_STRIP=$(find ${STAGING_BINDIR_NATIVE} -name "llvm-strip" | head -1)
    if [ -z "$LLVM_STRIP" ]; then
        bbfatal "llvm-strip not found in ${STAGING_BINDIR_NATIVE} -- check clang-native is in DEPENDS"
    fi
    find ${WORKDIR}/vkb_ramdisk -name "*.ko" -exec "$LLVM_STRIP" --strip-debug {} \;
    bbnote "stripped debug info from aarch64 kernel modules"

    # ─── Step 3: T5 DTB blob = 2 CONCATENATED DTBs (monaco-real + monacop). ───
    # The ABL selects the DTB by the hardware msm-id/board-id: monaco-real
    # (qcom,monaco, msm-id 486) + monacop (qcom,monacop, msm-id 517). Without the
    # matching one (monacop) it falls to EDL 05c6:900e (confirmed 2026-08-22 in
    # the ticwatch recipe). vkb-base.dtb (aurora) is no longer used.
    # monaco-real already carries ramoops@9ff00000 and splash_region — do not inject.
    # VIA1 V67-fix: force dr_mode=peripheral on the dwc3@4e00000 child. With
    # "otg" the core waits for the glue's role decision (extcon/io-channels of
    # the charger); without charger/EUD there is no cable -> the dwc3 never goes
    # out to the bus and the host does not enumerate (f_fs reads descriptors but
    # there is no physical USB signal). peripheral = direct gadget, without
    # waiting for extcon -> the UDC goes out to the bus.
    FDTPUT=$(find ${STAGING_BINDIR_NATIVE} -name fdtput 2>/dev/null | head -1)
    [ -n "$FDTPUT" ] || FDTPUT=$(command -v fdtput)
    test -n "$FDTPUT" || bbfatal "fdtput not found (dtc-native)"
    FDTGET=$(find ${STAGING_BINDIR_NATIVE} -name fdtget 2>/dev/null | head -1)
    [ -n "$FDTGET" ] || FDTGET=$(command -v fdtget)
    test -n "$FDTGET" || bbfatal "fdtget not found (dtc-native)"
    FDTOVERLAY="${STAGING_BINDIR_NATIVE}/fdtoverlay"
    test -n "$FDTOVERLAY" || bbfatal "fdtoverlay not found (dtc-native)"
    for dtb in monaco-real monacop; do
        cp ${S}/static/${dtb}.dtb ${WORKDIR}/${dtb}-per.dtb
        # dwc3 child path: /soc/hsusb@4e00000/dwc3@4e00000 (from the dts)
        "$FDTPUT" -t s ${WORKDIR}/${dtb}-per.dtb \
            /soc/hsusb@4e00000/dwc3@4e00000 dr_mode peripheral
        # VIA1: enable the eMMC (sdhc_1). In the stock DT it comes disabled — Wear
        # OS enables it via the board dtbo overlay; our boot does not apply those
        # overlays. Enable + supplies (phandles already verified: L25A=0x132,
        # L15A=0x182 in BOTH dtbs). Supplies = pm5100_l25 (3.08V, from the idp dtsi)
        # and pm5100_l15 (1.8V io). Without this sdhci stays deferred with no mmcblk0*.
        "$FDTPUT" -t s ${WORKDIR}/${dtb}-per.dtb /soc/sdhci@4744000 status ok
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb /soc/sdhci@4744000 vdd-supply 0x132
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb /soc/sdhci@4744000 vdd-io-supply 0x182
        # VIA1b: no OPP table for the sdhci — the OPP requires ICC paths
        # (required-opps) and without qnoc the paths are empty -> _opp_add_static_v2
        # fails (-22 'opp key field not found') and the probe aborts. Without OPP the
        # sdhci runs with the fixed ABL clocks (devfreq optional).
        "$FDTPUT" -d ${WORKDIR}/${dtb}-per.dtb /soc/sdhci@4744000 operating-points-v2
        # TOUCH: the T5 node is called "zinitix_ts@20" with
        # compatible = "zinitix,zinitix-ts", but the kernel driver
        # (drivers/input/touchscreen/zinitix.c) only accepts "zinitix,bt541"
        # -> the driver did not bind and there was NO /dev/input/eventX for touch
        # (the compositor started with evdevtouch:/dev/input/event2, which did not
        # exist; the only inputs were gpio-keys and qpnp_pon).
        # The node declares zinitix,pname="SM-G5308W" with x/y_resolution=0x1d1
        # (465, the panel's), i.e. it is a rebadged BT541: the compatible the
        # driver expects is added while ALSO keeping the original.
        "$FDTPUT" -t s ${WORKDIR}/${dtb}-per.dtb \
            /soc/qcom,qupv3_0_geni_se@4ac0000/i2c@4a84000/zinitix_ts@20 \
            compatible "zinitix,zinitix-ts" "zinitix,bt541"
        # TOUCH REGULATORS. The T5 node declares three PMIC rails
        # (vdd=0x84, vdd-v1=0x83, vcc_i2c=0x85) and the vendor driver turns all
        # three on. The mainline only asked for "vdd"+"vddo" (bulk get, which
        # FAILS if a property is missing) and with only those two the chip does
        # NOT answer at its i2c address:
        #   zinitix_start: "Error while sending power-on sequence: -107"
        #   (-107 = -ENOTCONN = I2C_ADDR_NACK, in drivers/i2c/busses/i2c-msm-geni.c)
        # The Zinitix "vddo" is its I/O rail, which here is "vcc_i2c" (0x85):
        # the vddo-supply property is created pointing there. And the driver is
        # patched to also request "vdd-v1" (0x83), so that all THREE rails are on.
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb \
            /soc/qcom,qupv3_0_geni_se@4ac0000/i2c@4a84000/zinitix_ts@20 \
            vddo-supply 0x85
        # zinitix_init_input_dev() calls touchscreen_parse_properties(), which
        # requires the STANDARD touchscreen-size-x/y props; the T5 DT only has
        # the vendor ones (zinitix,x_resolution/y_resolution = 0x1d1 = 465, which is
        # exactly the panel resolution). Without this:
        #   "Touchscreen-size-x and/or touchscreen-size-y not set in dts"
        #   probe failed with error -22
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb \
            /soc/qcom,qupv3_0_geni_se@4ac0000/i2c@4a84000/zinitix_ts@20 \
            touchscreen-size-x 0x1d1
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb \
            /soc/qcom,qupv3_0_geni_se@4ac0000/i2c@4a84000/zinitix_ts@20 \
            touchscreen-size-y 0x1d1
        # reset-gpios (STANDARD property): the T5 DT only has the vendor one
        # (zinitix,reset-gpio = <0x69 0x0c 0x00> = TLMM gpio 12). The patched
        # driver requests it with devm_gpiod_get_optional(..., "reset", ...) and
        # pulses the reset when starting the chip (without it the chip answered
        # over i2c but did not report touches). Flag 1 = GPIO_ACTIVE_LOW (normal
        # for a reset).
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb \
            /soc/qcom,qupv3_0_geni_se@4ac0000/i2c@4a84000/zinitix_ts@20 \
            reset-gpios 0x69 0x0c 0x1
        # ── OVERLAY FOR OUR VARIANT (Monaco IDP V1.0, board 0x10022) ────
        # Each SoC variant has its overlay in the dtbo (WDP -> Raydium,
        # IDP -> Zinitix...). The ABL applies them by board-id, but the watch's
        # (0x10022) is NOT applied (verified live: the touch node does not receive
        # the 'panel' property that the overlay adds). Here it is applied by hand
        # with fdtoverlay: the IDP V1.0 one adds to zinitix_ts@20 the link with
        # the panel (panel = <&dsi_rm69090_amoled_cmd>), which is what the vendor
        # driver uses to power on the chip.
        if [ -f ${S}/static/monaco-idp-v1-overlay.dtbo ]; then
            ${FDTOVERLAY} -i ${WORKDIR}/${dtb}-per.dtb \
                -o ${WORKDIR}/${dtb}-ovl.dtb \
                ${S}/static/monaco-idp-v1-overlay.dtbo \
                && mv ${WORKDIR}/${dtb}-ovl.dtb ${WORKDIR}/${dtb}-per.dtb \
                && bbnote "$dtb: overlay Monaco IDP V1.0 (board 0x10022) applied"
        else
            bbwarn "$dtb: missing monaco-idp-v1-overlay.dtbo"
        fi
        # ── WLAN EXPERIMENT (2026-09-26): aurora-style icnss ─────────────────
        # The ONLY deep difference from aurora (which has
        # WiFi) is that on dace the WLAN RF lives in the slate MCU
        # (`qcom,is_slate_rfa=1`, `rf_subtype=0`), while on aurora it lives in
        # the modem (`rf_subtype=1`/APACHE, no `is_slate_rfa`). The modem firmware
        # dies with DOG without its WLAN PD starting (it does not register
        # `wlan/fw` in the servloc). Hypothesis: the WLAN PD gets stuck waiting
        # for the slate RF. This experiment makes dace's icnss look like aurora's:
        #   - deletes `qcom,is_slate_rfa` (with `fdtput -d`; NOTE: setting it to
        #     0 does not work -- `of_property_read_bool` only checks if it EXISTS)
        #   - sets `qcom,rf_subtype = <1>` (WLFW_WLAN_RF_APACHE_V01)
        # This way icnss2 does not block waiting for the `slatefw` SSR and asks
        # for the APACHE RF. If the DOG disappears and `wlan0` appears, the
        # modem<->slate coupling is confirmed. REVERT the experiment = delete
        # these 2 lines.
        "$FDTPUT" -d ${WORKDIR}/${dtb}-per.dtb /soc/qcom,icnss@C800000 \
            qcom,is_slate_rfa
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb /soc/qcom,icnss@C800000 \
            qcom,rf_subtype 1
        bbnote "$dtb: WLAN experiment -- icnss without is_slate_rfa + rf_subtype=1 (APACHE)"
        # ── POWER / CHARGING: `dpdm-supply` from smblite is KEPT ────────────
        # The smblite uses `dpdm-supply = <&usb2_phy0>` (hsphy@1613000, phandle
        # 0x30) to enable the DPDM regulator registered by phy-msm-snps-hs:
        # it puts the PHY in "non-driving" mode so the PMIC completes the
        # charger detection (BC1.2/APSD). Without DPDM, POWER_PATH_STATUS has no
        # USE_USBIN -> `usb/online=0` -> ICL 2 mA -> **the watch does not
        # charge** (measured 2026-09-25: charging worked in the bootloader but
        # not in Linux). This property used to be deleted because the smblite
        # probe hung in smblite_lib_request_dpdm(); now the PHY driver already
        # carries the "dace B: no LDO control" patch (msm_hsphy_enable_power
        # returns without touching regulators if they are missing) and the DPDM
        # regulator is registered (regulator.56: hsphy@1613000), so it is tried
        # again. Reversible: if it hung again, restore the `fdtput -d`.
        bbnote "$dtb: smblite KEEPS dpdm-supply (DPDM charger detection)"
        # ── BT/WCN3988: the compatible decides the SOC in the HAL; the rails, if there is a chip ─
        # 1) COMPATIBLE: the HAL reads the FIRST one and derives the SOC type
        #    from it ("Soc version recevied : qcom,qcc5100" -> **slate**). With
        #    "qcom,wcn3990" first it picks **cherokee** (AP UART path, mute chip).
        #    qcc5100 is left FIRST and wcn3990 as second: the kernel drivers
        #    (btpower/btqca) match on ANY compatible in the list, so that second
        #    one serves their tables.
        # 2) RAILS: measured 2026-09-20 -- with soc=slate the HAL does NOT vote
        #    regulators ("FOR SLATE not voting any Regulators") and without these
        #    supplies `btpower` does not know which rails to turn on: pm5100_l13
        #    (core 1.304V) and pm5100_l17 (IO 1.8V) stayed at state=disabled and
        #    the chip was **UNPOWERED** -> it did not answer the handshake
        #    (opens ttyHS0 at 2400 bps and dies with InitTimeOut + err 0x55).
        #    Values from the stock DTSI monaco-standalone-idp-v1.dtsi:
        #    IO=L17A(0x184), core/RFA=L13A(0x181), PA/CH0=L26A(0x84), XO=L14A(0x131).
        "$FDTPUT" -t s ${WORKDIR}/${dtb}-per.dtb /soc/bt_wcn3990 \
            compatible "qcom,qcc5100" "qcom,wcn3990"
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb /soc/bt_wcn3990 \
            qcom,bt-vdd-io-supply 0x184
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb /soc/bt_wcn3990 \
            qcom,bt-vdd-core-supply 0x181
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb /soc/bt_wcn3990 \
            qcom,bt-vdd-pa-supply 0x84
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb /soc/bt_wcn3990 \
            qcom,bt-vdd-xtal-supply 0x131
        bbnote "$dtb: BT compatible qcc5100-first (=SOC slate) + io/core/pa/xtal rails"
        # ─ BT reset/enable: qcom,bt-sw-ctrl-gpio ────────────────────────────
        # Stock ships it but COMMENTED OUT (monaco-standalone-idp-v1.dtsi:
        # //qcom,bt-sw-ctrl-gpio = <&tlmm 69 GPIO_ACTIVE_HIGH>). The HAL asks
        # BT_CMD_CHECK_SW_CTRL and btpower has no gpio -> EINVAL
        # ('CheckSwCtrl: ioctl failed'). It is added like on the rest of
        # Qualcomm targets (tlmm 69 high). The controller phandle is read from
        # the dtb itself (here it is 0x69, not hardcoded).
        TLMM=$("$FDTGET" -t x ${WORKDIR}/${dtb}-per.dtb /soc/pinctrl@500000 phandle)
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb /soc/bt_wcn3990 \
            qcom,bt-sw-ctrl-gpio "$TLMM" 69 0
        bbnote "$dtb: bt-sw-ctrl-gpio = <&tlmm 69 0> (tlmm phandle=$TLMM)"
        # ── BT UART pinctrl: patched in the DRIVER (no hog in the DT) ───────
        # The UART node is left EXACTLY like stock/aurora. Verified 2026-09-19:
        # (a) the qupv3_se5_* groups are identical to aurora and the pinctrl-N
        # assignment too; (b) the 23259b2d hack (default/sleep = qup05) does NOT
        # mux (pins 26-29 stay at function=gpio during the whole HAL attempt);
        # (c) a HOG on /soc/pinctrl@500000 DOES mux to qup05 but CLAIMS the pins
        # and breaks the UART probe (msm_geni_serial loads with 0 ports:
        # /dev/ttyHS* never appears).
        # The solution is to force the mux from the driver itself in
        # msm_geni_serial_probe() -> dace-hs-uart-pinctrl.patch (in linux-dace).
        bbnote "$dtb: BT UART pinctrl is forced by the driver (dace-hs-uart-pinctrl.patch)"
        # ── /cont-splash-fb → /dev/fb0 over the continuous-splash ──────────
        # AURORA-STYLE (Step 3b of aurora-boot-images.bb): the bootloader paints
        # the logo in splash_region@0x5c000000 (label cont_splash_region) and the
        # SDE keeps scanning THAT region until the composer starts. The
        # qcom-cont-splash-fb driver (CONFIG_FB_QCOM_CONT_SPLASH=y, already in our
        # kernel with 0001-video-fbdev-...) exposes it as /dev/fb0, but ONLY if
        # the DT node exists: aurora injects it in its boot-images and we did not
        # have it.
        # Double use: (a) bring-up telemetry (the panel keeps the last color
        # painted after a hang = the only channel when there is no USB), and
        # (b) user splash (psplash) if ever needed.
        # Panel = 466x466 (rm69090-amoled-178-cmd), xRGB8888, stride 466*4.
        S_NODE=/cont-splash-fb
        "$FDTPUT" -c ${WORKDIR}/${dtb}-per.dtb "$S_NODE"
        "$FDTPUT" -t s ${WORKDIR}/${dtb}-per.dtb "$S_NODE" compatible "qcom,cont-splash-fb"
        "$FDTPUT" -t x ${WORKDIR}/${dtb}-per.dtb "$S_NODE" reg 0 0x5c000000 0 0x100000
        "$FDTPUT" -t u ${WORKDIR}/${dtb}-per.dtb "$S_NODE" width 466
        "$FDTPUT" -t u ${WORKDIR}/${dtb}-per.dtb "$S_NODE" height 466
        "$FDTPUT" -t u ${WORKDIR}/${dtb}-per.dtb "$S_NODE" stride 1864
        "$FDTPUT" -t s ${WORKDIR}/${dtb}-per.dtb "$S_NODE" format "x8r8g8b8"
        bbnote "$dtb: /cont-splash-fb node injected (fb0 = splash 0x5c000000, 466x466)"
        bbnote "$dtb: dr_mode=peripheral + sdhc_1 ok (vdd=l25/l15, without OPP) + RAYDIUM rm32380 @0x39 (i2c-1) + zinitix disabled + BT rails"
    done
    cat ${WORKDIR}/monaco-real-per.dtb ${WORKDIR}/monacop-per.dtb > ${WORKDIR}/dtb-blob-vendor.bin
    bbnote "DTB blob vendor_boot: $(stat -c%s ${WORKDIR}/dtb-blob-vendor.bin) bytes (stock=572016)"

    # ─── Step 4: ramdisk cpio + gzip ───
    # dace-vkb-assemble.py already left vkb_ramdisk/lib/modules/*.ko flat +
    # modules.dep (absolute paths) + modules.load(.recovery). Only pack into
    # cpio+gzip (T5 stock vendor_boot format: gzip -> cpio-newc). ───
    ( cd ${WORKDIR}/vkb_ramdisk && find . | sort | \
        cpio -o -H newc --owner root:root 2>/dev/null ) > ${WORKDIR}/vkb_rd.cpio
    gzip -9 -c ${WORKDIR}/vkb_rd.cpio > ${WORKDIR}/vkb_rd.gz

    MKBOOTIMG=${STAGING_BINDIR_NATIVE}/mkbootimg

    # ─── Step 5: mkbootimg vendor_kernel_boot.img (v4) ───
    # Replicates the ticwatch recipe (which boots): --base 0, full stock cmdline
    # (with bootconfig AND fw_devlink), --vendor_bootconfig per file.
    #
    # The QUP devices get their IOMMU domain from dace-iommu-defer.patch, so the
    # generic deferred_probe_timeout and arm_smmu.disable_bypass parameters are
    # deliberately not used (the latter cannot work: USFCFG is locked by TZ).
    #
    # blob of 2 DTBs. Without monacop the ABL falls to EDL.
    "${MKBOOTIMG}" \
        --header_version 4 --pagesize ${MKBOOTIMG_PAGESIZE} \
        --vendor_boot ${WORKDIR}/vendor_kernel_boot.img \
        --vendor_ramdisk ${WORKDIR}/vkb_rd.gz \
        --vendor_bootconfig ${S}/static/bootconfig \
        --dtb ${WORKDIR}/dtb-blob-vendor.bin \
        --base ${MKBOOTIMG_BASE} \
        --kernel_offset ${MKBOOTIMG_KERNEL_OFFSET} \
        --ramdisk_offset ${MKBOOTIMG_RAMDISK_OFFSET} \
        --tags_offset ${MKBOOTIMG_TAGS_OFFSET} \
        --dtb_offset ${MKBOOTIMG_DTB_OFFSET} \
        --vendor_cmdline 'lpm_levels.sleep_disabled=1 video=vfb:640x400,bpp=32,memsize=3072000 msm_rtb.filter=0x237 service_locator.enable=1 swiotlb=noforce kpti=off cgroup.memory=nokmem,nosocket loop.max_part=7 bootconfig qcom_geni_serial.con_enabled=0 androidboot.hardware=dace bootconfig buildvariant=user fw_devlink=permissive'

    # ─── Step 6: mkbootimg boot.img (v4): our kernel + empty ramdisk ───
    if [ ! -f "${LINUX_DACE_KIMG}" ]; then
        bbfatal "Kernel Image not at ${LINUX_DACE_KIMG}"
    fi
    : > ${WORKDIR}/empty_kernel
    : > ${WORKDIR}/empty_ramdisk
    "${MKBOOTIMG}" \
        --header_version 4 \
        --kernel "${LINUX_DACE_KIMG}" \
        --ramdisk ${WORKDIR}/empty_ramdisk \
        --cmdline '' \
        -o ${WORKDIR}/boot.img

    # ─── Step 7: mkbootimg init_boot.img (v4): asteroid initramfs as lz4 ───
    CPIO_GZ=$(ls -t ${DEPLOY_DIR_IMAGE}/initramfs-android-image-${MACHINE}-*.cpio.gz 2>/dev/null | head -1)
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
    ( cd ${DEPLOYDIR} && sha256sum boot.img vendor_kernel_boot.img init_boot.img > SHA256SUMS-dace )
    bbnote "Dace boot artifacts deployed: ${DEPLOYDIR}/{boot,vendor_kernel_boot,init_boot}.img"
}
addtask deploy after do_compile before do_build
