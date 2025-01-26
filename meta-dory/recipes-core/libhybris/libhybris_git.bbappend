# native_handle_clone is not declared on android-headers provider for `dory`
CFLAGS:append:dory = " -Wno-implicit-function-declaration -Wno-int-conversion"