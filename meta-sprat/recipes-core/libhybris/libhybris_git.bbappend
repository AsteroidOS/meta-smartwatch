# native_handle_clone is not declared on android-headers provider for `sprat`
CFLAGS:append:sprat = " -Wno-implicit-function-declaration -Wno-int-conversion"