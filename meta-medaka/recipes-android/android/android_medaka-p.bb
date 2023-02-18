inherit gettext

SUMMARY = "Downloads armv8a /usr/libexec/hal-droid and /usr/include/android folders and installs them for libhybris"
LICENSE = "CLOSED"

SRC_URI = "https://dodorad.io/tarball-generic-arm64.tar.gz"
SRC_URI[md5sum] = "b93c09dc4b10e80520962c3422e25d68"
SRC_URI[sha256sum] = "f18568e514ddd8e5aa52b3bef8e242291116a179a7691cdae5866e0981c5891d"
PV = "pie"
require recipes-android/android/android.inc
