#!/usr/bin/env python3
# dace kernel source fixes:
#   slatecom_interface.c: make ssr_register() conditional on
#   qcom_register_ssr_notifier being REACHABLE (built-in or module). The symbol
#   lives in QCOM_RPROC_COMMON, which is =m on dace; IS_BUILTIN was always true
#   and left the SSR notifier unregistered, so the slate MCU never got its
#   SPI_FREE notification (no glink, no BT, no crown).
import sys, os

def patch_slatecom(path):
    if not os.path.exists(path):
        print("slatecom: %s does not exist, skip" % path)
        return
    src = open(path).read()
    if 'IS_REACHABLE(CONFIG_QCOM_RPROC_COMMON)' in src:
        print("slatecom: already patched (IS_REACHABLE)")
        return
    new = '''static void ssr_register(void)
{
	int i;

	if (!IS_REACHABLE(CONFIG_QCOM_RPROC_COMMON)) {
		pr_info("ssr_register: QCOM_RPROC_COMMON not reachable, skip\\n");
		return;
	}

	for (i = 0; i < ARRAY_SIZE(service_data); i++) {'''
    # v1 (IS_BUILTIN) may have been injected by a previous build: replace it.
    old_v1 = '''static void ssr_register(void)
{
	int i;

	if (!IS_BUILTIN(CONFIG_QCOM_RPROC_COMMON)) {
		pr_info("ssr_register: QCOM_RPROC_COMMON not built-in, skip\\n");
		return;
	}

	for (i = 0; i < ARRAY_SIZE(service_data); i++) {'''
    if old_v1 in src:
        src = src.replace(old_v1, new, 1)
        open(path, 'w').write(src)
        print("slatecom: v1 (IS_BUILTIN) -> v2 (IS_REACHABLE) OK")
        return
    old = '''static void ssr_register(void)
{
	int i;

	for (i = 0; i < ARRAY_SIZE(service_data); i++) {'''
    if old not in src:
        sys.exit("slatecom: ssr_register not found")
    src = src.replace(old, new, 1)
    open(path, 'w').write(src)
    print("slatecom: ssr_register with IS_REACHABLE OK")

patch_slatecom(sys.argv[1])
