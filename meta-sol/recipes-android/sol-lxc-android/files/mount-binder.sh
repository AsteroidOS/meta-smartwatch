#!/bin/sh
# Second lxc.hook.mount: create /dev/{binder,hwbinder,vndbinder} inside the
# container. Without them servicemanager dies "cannot open /dev/binder" and
# Android init crash-loops it. lxc.hook.mount is cumulative, so this runs after
# the bundle's own mount.sh.
#
# Stash the real /apex before Android init hides it. This image carries
# flattened apexes (/apex holds the directories, /system/apex is empty; aurora
# is the opposite). init mounts a tmpfs over /apex for apexd to fill, which
# nothing does here, so every binary exec'd after that loses its linker:
#     init: cannot execv('/vendor/bin/sscrpcd'): No such file or directory
# This hook runs before init, so a bind to the unused /postinstall survives;
# the start script rebinds it over /apex once init's tmpfs is in place.
if [ -d "${LXC_ROOTFS_MOUNT}/apex" ] && [ -d "${LXC_ROOTFS_MOUNT}/postinstall" ]; then
    mount --bind "${LXC_ROOTFS_MOUNT}/apex" "${LXC_ROOTFS_MOUNT}/postinstall" 2>/dev/null \
        && echo "sol-lxc-android: stashed /apex -> /postinstall ($(ls "${LXC_ROOTFS_MOUNT}/postinstall" 2>/dev/null | wc -l) entries)"
fi

# Symlinks, never copies. A binderfs device is identified by its binderfs
# inode, not its major/minor, so a cp -a or mknod copy opens with ENXIO:
#     ProcessState: Opening '/dev/binder' failed: No such device or address
for n in binder hwbinder vndbinder; do
    if [ -e "/dev/binderfs/$n" ]; then
        rm -f "${LXC_ROOTFS_MOUNT}/dev/$n"
        ln -s "/dev/binderfs/$n" "${LXC_ROOTFS_MOUNT}/dev/$n" 2>/dev/null || true
        chmod 666 "/dev/binderfs/$n" 2>/dev/null || true
    fi
done

# Share the host's binderfs instance with the container. Binder contexts do not
# span binderfs instances, so if init mounted its own, libhybris on the host
# could never see the composer the container registers; sharing the IPC
# namespace is not enough. init's `mount binder binder /dev/binderfs` is
# neutered by the start script's init.rc injection. Because this runs before
# init, init's chmod 0666 on /dev/binderfs/* lands on the host's nodes (shipped
# 0600), which container services need.
if [ -d /dev/binderfs ] && [ -d "${LXC_ROOTFS_MOUNT}/dev/binderfs" ]; then
    mount --bind /dev/binderfs "${LXC_ROOTFS_MOUNT}/dev/binderfs" 2>/dev/null \
        && echo "sol-lxc-android: shared host binderfs into container"
elif [ -d /dev/binderfs ]; then
    mkdir -p "${LXC_ROOTFS_MOUNT}/dev/binderfs" 2>/dev/null
    mount --bind /dev/binderfs "${LXC_ROOTFS_MOUNT}/dev/binderfs" 2>/dev/null \
        && echo "sol-lxc-android: shared host binderfs into container (created mountpoint)"
fi
