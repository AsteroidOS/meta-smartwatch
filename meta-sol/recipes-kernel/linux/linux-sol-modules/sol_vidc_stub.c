// SPDX-License-Identifier: GPL-2.0-only
/*
 * sol_vidc_stub: bind the Venus video node so GCC can reach sync_state.
 *
 * sol does not build msm_video (it hung boot via v4l_id during bring-up and
 * needs mmrm, which does not build on this branch). With 5a00000.qcom,vidc
 * left unbound, fw_devlink keeps gcc-monaco's sync_state from ever running, so
 * clk_sync_state() never drops the references clk_core_hold_state() took on
 * every clock the bootloader left on (USB, eMMC, the debug UART, gpll0), and
 * they stay enabled through system suspend. Binding a do-nothing driver here
 * completes the consumer set. Displaced the day msm_video is really built.
 */
#include <linux/module.h>
#include <linux/of.h>
#include <linux/platform_device.h>

static int sol_vidc_stub_probe(struct platform_device *pdev)
{
	dev_info(&pdev->dev, "video codec not supported; bound so GCC can sync its state\n");
	return 0;
}

static const struct of_device_id sol_vidc_stub_match[] = {
	{ .compatible = "qcom,msm-vidc" },
	{ }
};
MODULE_DEVICE_TABLE(of, sol_vidc_stub_match);

static struct platform_driver sol_vidc_stub_driver = {
	.probe = sol_vidc_stub_probe,
	.driver = {
		.name = "sol_vidc_stub",
		.of_match_table = sol_vidc_stub_match,
	},
};
module_platform_driver(sol_vidc_stub_driver);

MODULE_DESCRIPTION("Placeholder driver for sol's unused Venus video node");
MODULE_LICENSE("GPL");
