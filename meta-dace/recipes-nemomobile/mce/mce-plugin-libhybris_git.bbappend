# dace: the original meta-asteroid recipe only declares
#     DEPENDS += "mce libmce-glib"
# and do_compile fails with:
#     plugin-config.h:32:11: fatal error: glib.h: No such file or directory
# because glib-2.0 is not in its sysroot (it only arrived as a dependency of
# libmce-glib, which is not enough for the headers). Also, being mce's "libhybris
# plugin", it needs the libhybris headers (the plugin talks to the Android HAL
# through it).
DEPENDS += "glib-2.0 libhybris"
