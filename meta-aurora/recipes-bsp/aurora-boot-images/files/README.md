# aurora-boot-images source files

This directory holds the per-port static inputs that
`aurora-boot-images.bb` consumes to produce `boot.img`,
`vendor_kernel_boot.img`, and `init_boot.img`. The Python orchestrator
and manifest are editable; the `static/` files are extracted from the
stock device and shouldn't change unless Google reflashes the watch.

## Files

### `aurora-vkb-assemble.py`

Builds the ramdisk that gets baked into `vendor_kernel_boot.img`:
flattens the kernel `.ko`'s, copies the techpack `.ko`'s, generates a
`modules.dep`-aware layout, and stages `modules.load`, `modules.alias`,
`modules.softdep`, `modules.load.charger`, and the DTB. Driven from
`aurora-vkb-modules.lst`. KMI-CRC-checks every module against the
kernel's `Module.symvers` so we catch ABI breaks at build time.

### `aurora-vkb-modules.lst`

Manifest of `.ko`'s that go into `vendor_kernel_boot.img`. Each entry
is `<name>:k` (kernel-built, from linux-aurora's kernel-modules
package) or `<name>:tp` (techpack-built, from the linux-aurora-modules
ipk); `@drop <name>` documents a stock module intentionally not
shipped, and `@dep <child>: <parents>` pins/overrides a modules.dep
line. Hand-edited; this is the only place that says what ships at
early boot (what *loads* is init_boot's `modules.load.aurora`).

## `static/` — extracted from the stock device

Each file here was lifted from a working stock UBPorts install on
this hardware. They're not buildable from source in Yocto, so we ship
them verbatim.

### `vkb-base.dtb`

Stock device tree, extracted from a working
`vendor_kernel_boot.img`. The kernel doesn't build dtbs from
`gki_defconfig`, and the bootloader requires the original `qcom,msm-id`
and `qcom,board-id` properties to be present and unchanged — so we
ship the bootloader-blessed blob and only patch the runtime ramoops
node into it at `do_compile` time.

How to regenerate (from a fastboot-unlocked Pixel Watch 2):

```sh
# 1. Dump the partition:
adb shell 'su -c dd if=/dev/block/by-name/vendor_kernel_boot of=/sdcard/vkb.img'
adb pull /sdcard/vkb.img

# 2. Extract the DTB section with unpack_bootimg (from mkbootimg-tools):
unpack_bootimg --boot_img vkb.img --out vkb_unpacked
cp vkb_unpacked/dtb vkb-base.dtb
```

The current `vkb-base.dtb` is from `vkb_b` on slot B.

### `modules.dep`, `modules.alias`, `modules.softdep`

`depmod` output from the stock UBPorts ramdisk. The kernel build
generates fresh `modules.dep`/`modules.alias` for our own `.ko` set at
install time, but the bootloader's charger-mode path and the
vendor_kernel_boot's first-stage init both expect a `depmod`-compatible
layout to be present from the start. We ship the stock layouts so the
on-device tooling sees the file shapes it expects; the assembler
overwrites with kernel-build output during `do_compile`.

How to regenerate:

```sh
# From a running UBPorts install:
adb pull /lib/modules/5.15.144/modules.dep
adb pull /lib/modules/5.15.144/modules.alias
adb pull /lib/modules/5.15.144/modules.softdep
```

### `modules.load.charger`

List of `.ko`'s that the stock charger-mode `init.rc` modprobes when
the device boots into the low-power charger UI (i.e. after plugging
in a powered-off watch). We don't ship a charger-mode init ourselves
yet, but the file lives here for parity with the stock partition
layout and so the manifest can reference it.

Pulled from the stock UBPorts `vendor_kernel_boot/lib/modules/` dir.

## When porting to a new SoC/board

1. `aurora-vkb-modules.lst` — rebuild the manifest from the new
   board's kernel + techpack module set
2. `vkb-base.dtb` — extract from the new device's
   `vendor_kernel_boot` partition (see above)
3. `modules.dep`/`alias`/`softdep` — generate fresh from a known-good
   userspace install
4. `modules.load.charger` — copy if the bootloader has charger mode,
   else omit
5. Adjust `RAMOOPS_BASE`/`SIZE` in `aurora-boot-images.bb` to match
   the new device's reserved-memory layout
6. Adjust `MKBOOTIMG_*` offsets if the new SoC uses a different
   memory map

`aurora-vkb-assemble.py` itself is board-agnostic and shouldn't need
porting.
