# native_handle_clone is not declared on android-headers provider for `swift`
CFLAGS:append:swift = " -Wno-implicit-function-declaration -Wno-int-conversion"