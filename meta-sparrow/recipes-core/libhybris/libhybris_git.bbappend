# native_handle_clone is not declared on android-headers provider for `sparrow`
CFLAGS:append:sparrow = " -Wno-implicit-function-declaration -Wno-int-conversion"
