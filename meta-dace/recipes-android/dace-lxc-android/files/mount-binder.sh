#!/bin/sh
# LXC mount hook: ensure /dev/{binder,hwbinder,vndbinder} resolve inside
# the container. The host bind-mounts /dev/binderfs into the container
# via lxc.mount.entry; this hook creates the per-node symlinks Android
# expects -- servicemanager, vndservicemanager and hwservicemanager all
# open these by name and init crash-loops with "cannot open /dev/binder"
# without them.
DEV="${LXC_ROOTFS_MOUNT}/dev"
mkdir -p "$DEV"
for node in binder hwbinder vndbinder; do
    ln -sf "binderfs/$node" "$DEV/$node" 2>/dev/null || true
done
