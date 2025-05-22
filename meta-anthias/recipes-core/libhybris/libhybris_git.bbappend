# native_handle_clone is not declared on android-headers provider for `anthias`
CFLAGS:append:anthias = " -Wno-implicit-function-declaration -Wno-int-conversion"