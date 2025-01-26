# native_handle_clone is not declared on android-headers provider for `sturgeon`
CFLAGS:append:tetra = " -Wno-implicit-function-declaration -Wno-int-conversion"