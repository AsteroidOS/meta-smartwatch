# libhybris and /system/ directory build instructions


AsteroidOS utilizes libhybris in combination with Android libraries to take full advantage of the hardware. libhybris is a tool that allows us to use Android libraries on a non-Android userspace system.

This document describes how to prepare the Android libraries from the /system partition and how to compile patched Android libraries for use with libhybris.
With this there is a distinction between the device specific /system partition and generic Android libraries patched for libhybris. This essentially creates two tarballs: a system tarball and a patched Android libraries tarball.
The system tarball only contains a single folder (system). While the tarball for libhybris contains two folders (/include/ and /usr/libexec/hal-droid/system/).

## 1. Get the a dump of the system partition

Keep in mind that this document targets Android Marshmallow (6.0.1) systems. If possible downgrade/upgrade your system to Android Marshmallow before continuing.

The recommended way to get a system partition dump is to use TWRP and pull the entire partition using ADB: `adb pull /dev/block/platform/msm_sdcc.1/by-name/system system.img` also be sure to get a dump of the firmware partition: `adb pull /dev/block/platform/msm_sdcc.1/by-name/modem modem.img`. The firmware dump is needed for booting the aDSP (Audio processor).

### 1.1 Extract the dumps

Now extract these dumps and merge them as follows:
```sh
mkdir tmp
sudo mount system.img tmp
sudo cp -r tmp system
sudo umount tmp
sudo mount modem.img tmp
sudo cp -r tmp firmware
```
This results in two folders: system and firmware.


### 1.2 Prepare /system

We can now copy the firmware files to the proper directory and adjust some symlinks:
```sh
cp -r firmware/image/* system/vendor/firmware/

cd system/
rm -rf app/ fonts/ framework/ media/ priv-app/ xbin/
sed -i "/persist.hwc.mdpcomp.enable=true/d" build.prop
cd vendor/lib/egl/
ln -s libGLESv2_adreno.so libGLESv2S3D_adreno.so
cd ../../../
```

### 1.3 Create the system tarball

```sh
tar zcvf system-sturgeon-m.tar.gz system
```

## 2. Patch bionic and GPU drivers

libhybris requires a patched bionic and GPU drivers with the QCOM_BSP flag enabled. These parts are open-source so we can rebuild them.

### 2.1 Download repo

Download all the files needed for compilation:
``` sh
curl https://storage.googleapis.com/git-repo-downloads/repo > repo
chmod a+x repo
mkdir -p android-hybris/
cd android-hybris

../repo init -u https://github.com/AsteroidOS/android_manifest -b marshmallow-dr1.5-release -g all,-notdefault,-darwin,-mips --depth=1
../repo sync
```

### 2.2 Build

Build everything:
```sh
. build/envsetup.sh
export TARGET_USES_C2D_COMPOSITION=true # Needed by copybit
export TARGET_USES_QCOM_BSP=true        # Fixes GPU problems on QCOM platforms
export TARGET_BOARD_PLATFORM=msm8226    # We Specify the SoC by hand
export QCOM_BOARD_PLATFORMS=msm8226
export PLATFORM_VERSION=6.0.1

mmma hardware/qcom/display/msm8226/     # hwcomposer, gralloc, dependencies...
mmma frameworks/native/cmds/servicemanager/
mmma system/core/logd/
mmma system/core/init/
```

### 2.3 Installation

Make sure that most libraries are separated from the original libraries from Android Wear.
```sh
mkdir -p ../usr/libexec/hal-droid/system/bin/
cp out/target/product/generic/root/init ../usr/libexec/hal-droid/system/bin/
cp -r out/target/product/generic/system/bin/* ../usr/libexec/hal-droid/system/bin/
chmod +x ../usr/libexec/hal-droid/system/bin/*

mkdir -p ../usr/libexec/hal-droid/system/
cp -r out/target/product/generic/system/usr/ ../usr/libexec/hal-droid/system/

mkdir -p ../usr/libexec/hal-droid/system/lib/
cp -r out/target/product/generic/system/lib ../usr/libexec/hal-droid/system
cd ../
```

## 3. libhybris headers

The second part of this tarball contains header files to compile against
libhybris. There is a script in the libhybris distribution that can pull
the header files from the downloaded android distribution (above).
```sh
git clone https://github.com/libhybris/libhybris
./libhybris/utils/extract-headers.sh -v 6.0.1 -p /usr/include/android android-hybris include

```

## 4. Putting it all together

With the system and include directories, you can create the system-dir tarball:
```sh
tar zcvf hybris-m-msm8226.tar.gz include usr
```