# sol first-stage module load order.
#
# Captured from stock sol (AW2A.240903.001.A4): the order libmodprobe logged
# ('init: Loading module ...'), which honours modules.dep and modules.softdep
# (qcom_hwspinlock before smem).
#
# Not interchangeable with aurora's list: different board, different modules.
# busybox modprobe does not resolve dependencies here, so the order matters.
qcom-cpufreq-hw.ko
sched-walt.ko
qcom_hwspinlock.ko
smem.ko
minidump.ko
qcom_ipc_logging.ko
msm_sysstats.ko
qcom-mpm.ko
qcom-scm.ko
pinctrl-msm.ko
pinctrl-monaco.ko
proxy-consumer.ko
debug-regulator.ko
gdsc-regulator.ko
clk-qcom.ko
clk-dummy.ko
rpm-smd.ko
clk-smd-rpm.ko
dispcc-monaco.ko
gcc-monaco.ko
gpucc-monaco.ko
qcom_aoss.ko
mdt_loader.ko
smp2p.ko
secure_buffer.ko
socinfo.ko
mem_buf_dev.ko
mem_buf.ko
dcc_v2.ko
panel_event_notifier.ko
# --- QCOM Apps Watchdog: not loaded -------------------------------------------
# These arm a hardware watchdog (bark 11 s, pet 9.36 s) that Android pets via
# /dev/watchdog and nothing in AsteroidOS does. Loaded, the watch resets about
# 11 s into Linux with no panic in the log. To bring it back, have something pet
# it (systemd RuntimeWatchdogSec=) rather than re-adding these.
# qcom_wdt_core.ko
# qcom_soc_wdt.ko
memory_dump_v2.ko
qcom_logbuf_vh.ko
qcom-pmu-lib.ko
qcom-dcvs.ko
hwkm_v1.ko
crypto-qti-hwkm.ko
crypto-qti-common.ko
qcom_cpu_vendor_hooks.ko
msm_performance.ko
rpm-smd-regulator.ko
stub-regulator.ko
qcom_iommu_util.ko
iommu-logger.ko
arm_smmu.ko
msm_dma_iommu_mapping.ko
qti-regmap-debugfs.ko
qseecom_proxy.ko
qcom-spmi-pmic.ko
qcom_dma_heaps.ko
spmi-pmic-arb.ko

# --- PMIC GPIO + charger chain: the supplier of the USB extcon -----------------
# Stock loads these from second-stage init; AsteroidOS has no second stage.
#
# google-extcon-shim resolves `google,supplier-extcon`, which on sol is
# qcom,qpnp-smblite (pm5100_charger). Until smblite registers its extcon the
# shim defers silently, msm-dwc3 never binds 4e00000.hsusb and adb never comes
# up. Loading the chain here, before dwc3-msm, puts the supplier in place first.
#
# pinctrl-spmi-gpio is the PMIC GPIO provider; without it gpio-keys hits
# "deferred probe timeout, ignoring dependency".
#
# Order is topological per modules.dep, not stock's modules.load.charger order
# (which lists qcom-spmi-adc5-gen3 before qcom-vadc-common).
pinctrl-spmi-gpio.ko
qcom-vadc-common.ko
qcom-spmi-adc5-gen3.ko
gvotable.ko
logbuffer.ko
qpnp-smblite-main.ko
qti-qbg-main.ko
google-bms.ko
google-battery.ko
google-charger.ko

# sw5100_bms.ko needs QBG's IIO channels at probe time, so a udev rule loads it
# on QBG's add event (71-sol-battery-gauge.rules).
#
# qpnp-power-on (side button) is loaded from modules-load.d instead, see
# sol-post-rootfs.conf.

phy-msm-snps-hs.ko
phy-generic.ko
eud.ko
dwc3-msm.ko
rtc-pm8xxx.ko
qcom-dload-mode.ko
qcom-reboot-reason.ko
cpu_hotplug.ko
bcl_pmic5.ko
sdhci-msm-scaling.ko
cqhci.ko
sdhci-msm.ko
qcom-apcs-ipc-mailbox.ko
qcom_glink.ko
qcom_glink_smem.ko
qcom_smd.ko
rproc_qcom_common.ko
qcom_glink_rpm.ko
nvmem_qcom-spmi-sdam.ko
icc-rpm.ko
qnoc-qos-rpm.ko
qnoc-monaco.ko
coresight.ko
qrtr.ko
nanohub.ko
mcu_display.ko
msm_drm.ko
msm_kgsl.ko
qseecom_dlkm.ko
smcinvoke_dlkm.ko
