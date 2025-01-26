SRC_URI:append:nemo = " file://0001-Report-support-for-only-2.0-instead-of-3.0.patch;striplevel=2"
# native_handle_clone is not declared on android-headers provider for `nemo`
CFLAGS:append:nemo = " -Wno-implicit-function-declaration -Wno-int-conversion"
