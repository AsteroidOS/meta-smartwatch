# native_handle_clone is not declared on android-headers provider for `smelt`
CFLAGS:append:smelt = " -Wno-implicit-function-declaration -Wno-int-conversion"
