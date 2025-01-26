# native_handle_clone is not declared on android-headers provider for `sawfish`
CFLAGS:append:sawfish = " -Wno-implicit-function-declaration -Wno-int-conversion"