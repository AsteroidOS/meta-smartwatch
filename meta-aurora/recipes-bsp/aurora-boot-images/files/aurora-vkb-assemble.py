#!/usr/bin/env python3
# Assemble vendor_kernel_boot.img's ramdisk directory from a declarative
# manifest. Standalone so it's testable outside bitbake.
#
# Usage:
#   aurora-vkb-assemble.py \
#       --manifest aurora-vkb-modules.lst \
#       --kernel-pkgdir <linux-aurora's package/usr/lib/modules/5.15.144/kernel> \
#       --techpack-dir  <linux-aurora-modules ipk-extracted root> \
#       --symvers       <kernel's Module.symvers> \
#       --static-dir    <static metadata: modules.alias etc.> \
#       --out           <ramdisk staging dir>
#
# Exits non-zero on any KMI-CRC mismatch, missing module, or @drop entry whose
# .ko was accidentally also listed.

import argparse
import os
import re
import shutil
import struct
import subprocess
import sys
from pathlib import Path


def find_ko(roots, name):
    """Find <name>.ko under any of roots (excluding .debug/), trying
    dash/underscore variants. Returns the first match or None."""
    variants = [name, name.replace("_", "-"), name.replace("-", "_")]
    for root in roots:
        if not root or not os.path.isdir(root):
            continue
        for variant in variants:
            for dirpath, _, files in os.walk(root):
                if "/.debug" in dirpath or dirpath.endswith("/.debug"):
                    continue
                fname = f"{variant}.ko"
                if fname in files:
                    return os.path.join(dirpath, fname)
    return None


def parse_manifest(path):
    """Yields (kind, payload) tuples. kind in {'module', 'drop', 'dep'}."""
    with open(path) as f:
        for lineno, raw in enumerate(f, 1):
            line = raw.split("#", 1)[0].strip()
            if not line:
                continue
            if line.startswith("@drop "):
                yield ("drop", line[len("@drop "):].strip())
                continue
            if line.startswith("@dep "):
                # @dep child.ko: parent1.ko parent2.ko
                body = line[len("@dep "):]
                if ":" not in body:
                    sys.exit(f"manifest:{lineno}: @dep missing ':' -- {raw!r}")
                child, rest = body.split(":", 1)
                yield ("dep", (child.strip(), rest.strip()))
                continue
            if ":" not in line:
                sys.exit(f"manifest:{lineno}: missing src tag (:k or :tp) -- {raw!r}")
            name, src = line.rsplit(":", 1)
            name, src = name.strip(), src.strip()
            if src not in ("k", "tp"):
                sys.exit(f"manifest:{lineno}: src tag must be 'k' or 'tp', got {src!r}")
            yield ("module", (name, src))


# Find the module_layout symbol's CRC inside a .ko. The kernel writes CRC32s as
# little-endian u32 immediately before each __ksymtab_strings entry; finding
# "module_layout\0" in the strings and reading back 8 bytes (CRC + alignment
# pad on some kernels, but the CRC is the low u32) matches what reassemble.sh's
# KMI gate does.
MOD_LAYOUT_SYM = b"module_layout\x00"


def module_layout_crc(ko_path):
    data = Path(ko_path).read_bytes()
    m = re.search(MOD_LAYOUT_SYM, data)
    if not m or m.start() < 8:
        return None
    # 8 bytes before the symbol string contains the CRC (u32 little-endian)
    # followed by padding. Low u32 is the CRC.
    return struct.unpack("<I", data[m.start() - 8:m.start() - 4])[0]


def kernel_module_layout_crc(symvers_path):
    """Read the kernel-side module_layout CRC from Module.symvers."""
    with open(symvers_path) as f:
        for line in f:
            parts = line.split()
            if len(parts) >= 2 and parts[1] == "module_layout":
                return int(parts[0], 16) & 0xFFFFFFFF
    return None


# Only modules in this set get strip --strip-debug. Everything else ships at
# the size it landed in $FND (foundational `:k` modules come pre-stripped by
# OE's split-debug behavior) or the techpack ipk (techpack mods have debug info
# but it doesn't blow the partition cap, except for wlan.ko).
#
# Why not strip everything? Empirically: stripping msm_drm.ko and msm_kgsl.ko
# bricks boot at the G logo on aurora -- likely CFI / build metadata in
# non-.debug_* sections that --strip-debug nonetheless drops on this
# clang/kernel combo. Only wlan.ko genuinely *needs* stripping (it's 333 MB
# raw; everything else fits within the 64 MB lz4-cpio cap unstripped).
STRIP_MODULES = {"wlan"}


def strip_debug(ko_path, strip_bin):
    """Run host strip --strip-debug if this module is in STRIP_MODULES."""
    name = Path(ko_path).stem
    if name not in STRIP_MODULES:
        return
    try:
        subprocess.run(
            [strip_bin, "--strip-debug", ko_path],
            check=True, capture_output=True
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        # Non-fatal; .ko stays at full size. Larger ramdisk but bootable.
        pass


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--manifest", required=True)
    ap.add_argument("--kernel-pkgdir", required=True,
                    help="linux-aurora's package/usr/lib/modules/<v>/kernel")
    ap.add_argument("--techpack-dir", required=True,
                    help="linux-aurora-modules ipk-extracted root")
    ap.add_argument("--symvers", required=True,
                    help="Kernel Module.symvers (for module_layout CRC)")
    ap.add_argument("--static-dir", required=True,
                    help="Stock metadata: modules.alias, modules.softdep, modules.load.charger")
    ap.add_argument("--out", required=True,
                    help="Output ramdisk staging dir (will be wiped)")
    ap.add_argument("--strip", default="/usr/bin/strip")
    args = ap.parse_args()

    out = Path(args.out)
    if out.exists():
        shutil.rmtree(out)
    md = out / "lib" / "modules"
    md.mkdir(parents=True)

    # KMI reference
    kcrc = kernel_module_layout_crc(args.symvers)
    if kcrc is None:
        sys.exit(f"ABORT: module_layout CRC not in {args.symvers}")
    print(f"== kernel module_layout CRC = 0x{kcrc:08x}")

    modules_in_order = []   # [(name, ko_path, src)]
    dropped = set()
    deps = []               # [(child, parents_str)]

    for kind, payload in parse_manifest(args.manifest):
        if kind == "drop":
            dropped.add(payload)
        elif kind == "dep":
            deps.append(payload)
        else:
            name, src = payload
            if name in dropped:
                sys.exit(f"ABORT: manifest lists {name} but @drop'd earlier")
            roots = ([args.kernel_pkgdir] if src == "k"
                     else [args.techpack_dir])
            ko = find_ko(roots, name)
            if not ko:
                # Fallback search the other source. Helps catch a misclassified
                # tag rather than failing with a generic "not built".
                fallback = ([args.techpack_dir] if src == "k"
                            else [args.kernel_pkgdir])
                ko2 = find_ko(fallback, name)
                hint = (f" (found in {'techpack' if src == 'k' else 'kernel-pkg'}"
                        f" -- wrong :{src} tag?)" if ko2 else "")
                sys.exit(f"ABORT: {name}.ko not built in {src}-source{hint}")
            modules_in_order.append((name, ko, src))

    # Copy + strip + KMI-gate
    failed = []
    for name, ko_src, src in modules_in_order:
        dst = md / f"{name}.ko"
        shutil.copy(ko_src, dst)
        os.chmod(dst, 0o644)
        strip_debug(str(dst), args.strip)
        crc = module_layout_crc(str(dst))
        if crc != kcrc:
            failed.append((name, src, f"0x{crc:08x}" if crc else "none"))
    if failed:
        for n, s, c in failed:
            print(f"  KMI FAIL: {n}.ko ({s}) module_layout={c} (expected 0x{kcrc:08x})")
        sys.exit(f"ABORT: KMI-CRC gate failed for {len(failed)} module(s)")
    print(f"== KMI gate PASSED ({len(modules_in_order)} modules @ CRC 0x{kcrc:08x})")

    # modules.load + modules.load.recovery
    load_lines = [f"{n}.ko\n" for (n, _, _) in modules_in_order]
    (md / "modules.load").write_text("".join(load_lines))
    (md / "modules.load.recovery").write_text("".join(load_lines))
    print(f"== modules.load + modules.load.recovery: {len(load_lines)} entries")

    # modules.dep -- start from the stock vkbrd modules.dep (with FULL
    # symbol-dep info for the 96 foundational modules), then apply @dep
    # overrides from the manifest. Empty deps would make modprobe attempt naked
    # insmod and the kernel rejects with "Unknown symbol", which we observed
    # brick boot at 'init: Waiting for mmcblk0p82' (sdhci-msm needed
    # crypto_qti_disable / cqhci_*; clk-qcom needed gdsc_*; etc.).
    #
    # Stock format: "/lib/modules/X.ko: /lib/modules/Y.ko /lib/modules/Z.ko"
    # Manifest @dep format: "X.ko: Y.ko Z.ko" -- we normalise the bare names to
    # the /lib/modules/ prefix for consistency.
    PFX = "/lib/modules/"
    dep_map = {}  # key = "<X.ko>" (no prefix), value = list of "<Y.ko>" deps
    static = Path(args.static_dir)

    stock_dep = static / "modules.dep"
    if stock_dep.is_file():
        with stock_dep.open() as f:
            for line in f:
                line = line.strip()
                if not line or ":" not in line:
                    continue
                key, rest = line.split(":", 1)
                key = key.strip()
                key = key[len(PFX):] if key.startswith(PFX) else key
                if key in dropped:
                    continue
                ps = [p.strip() for p in rest.split() if p.strip()]
                ps = [p[len(PFX):] if p.startswith(PFX) else p for p in ps]
                ps = [p for p in ps if p[:-3] not in dropped]  # strip ".ko" before drop check
                dep_map[key] = ps
        print(f"== stock modules.dep: {len(dep_map)} entries seeded "
              f"(dropped: {len(dropped)})")

    # Ensure every module listed in the manifest has at least an entry (empty
    # deps is acceptable for modules with no symbol-deps).
    for name, _, _ in modules_in_order:
        ko = f"{name}.ko"
        if ko not in dep_map:
            dep_map[ko] = []

    # @dep overrides from the manifest. Manifest uses bare names; normalise.
    overridden = 0
    for child, parents in deps:
        child = child.strip()
        if child not in dep_map:
            print(f"  WARN: @dep {child} not in module list -- still recording")
            dep_map[child] = []
        parsed = [p.strip() for p in parents.split() if p.strip()]
        # Allow either bare or prefixed in the manifest
        parsed = [p[len(PFX):] if p.startswith(PFX) else p for p in parsed]
        dep_map[child] = parsed
        overridden += 1

    with (md / "modules.dep").open("w") as f:
        for ko_name, deps_list in dep_map.items():
            parts = [PFX + p for p in deps_list]
            f.write(f"{PFX}{ko_name}:" + ("" if not parts else " " + " ".join(parts)) + "\n")
    print(f"== modules.dep: {len(dep_map)} entries "
          f"({overridden} @dep overrides applied)")

    # Static metadata: ship as-is.
    static = Path(args.static_dir)
    for name in ("modules.alias", "modules.softdep", "modules.load.charger"):
        src = static / name
        if src.is_file():
            shutil.copy(src, md / name)
            print(f"== shipped static {name}")
        else:
            print(f"  WARN: static {name} missing")

    # Size summary
    total = sum(f.stat().st_size for f in md.glob("*.ko"))
    print(f"== ramdisk content: {len(modules_in_order)} .ko (stripped {total} bytes)")


if __name__ == "__main__":
    main()
