# sol's GNSS HAL is AIDL, so it needs the binder provider, not the libhybris
# one. android.hardware.gnss-aidl-service-qti.rc registers
# android.hardware.gnss.IGnss/default, and the binder backend asks for that
# name without version gating, so sol's version 3 is reachable. /dev/binder
# (a symlink to the container's /dev/binderfs/binder) and libgbinder are
# already in place for sensorfw.
RDEPENDS:${PN}:append:sol = " geoclue-provider-hybris-binder "
