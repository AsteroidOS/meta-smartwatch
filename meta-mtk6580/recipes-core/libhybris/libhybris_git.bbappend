# native_handle_clone is not declared on android-headers provider for `harmony`
CFLAGS:append:harmony = " -Wno-implicit-function-declaration -Wno-int-conversion"
# native_handle_clone is not declared on android-headers provider for `inharmony`
CFLAGS:append:inharmony = " -Wno-implicit-function-declaration -Wno-int-conversion"

