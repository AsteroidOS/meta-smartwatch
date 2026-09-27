Stock vendor modules (Mobvoi msm-5.15 g7f9d6c16b5cd) PATCHED for our kernel
(dace). Origin: ota-stock/extracted/modules-stripped/*.ko
(already without the __versions section -> they load with forced taint).

PATCHES APPLIED (see patch-stock-module.py in the repo root):
1. SCS (shadow call stack): the stock .ko's save x30 via x18
   (str x30,[x18],#8 / ldr x30,[x18,#-8]!). Our kernel does NOT have
   CONFIG_SHADOW_CALL_STACK (sw5100.fragment asks for it but Kconfig
   discards it), so x18 is garbage and those writes corrupt memory ->
   the rootfs booted, crashed and went back to safe mode. The push/pop
   pairs are NOPed in .text/.init.text/.exit.text.
2. exit reloc in .gnu.linkonce.this_module: 0x378 (stock) -> 0x3a8
   (our kernel). The init one is NOT touched: our layout puts it at
   0x178, same as stock (thanks to the ABI slot
   dace-module-cfi-abi-slot.patch). It only affects rmmod.

Regenerate:
  python3 patch-stock-module.py <in.ko> <out.ko>
  (by default: init 0x178->0x178, exit 0x378->0x3a8, NOP SCS)
If the struct module layout changes (measure it by building a test module
against the build dir and dumping the this_module relocs), pass the new
offsets as the 3rd/4th argument.
