SRC_URI:append:lenok = " file://0001-Report-support-for-only-2.0-instead-of-3.0.patch;striplevel=2"
# native_handle_clone is not declared on android-headers provider for `lenok`
CFLAGS:append:lenok = " -Wno-implicit-function-declaration -Wno-int-conversion"
