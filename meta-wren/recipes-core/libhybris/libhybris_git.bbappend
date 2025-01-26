# native_handle_clone is not declared on android-headers provider for `wren`
CFLAGS:append:wren = " -Wno-implicit-function-declaration -Wno-int-conversion"