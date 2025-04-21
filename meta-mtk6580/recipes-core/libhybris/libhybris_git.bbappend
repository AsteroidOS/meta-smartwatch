# native_handle_clone is not declared in headers and gps_callback uses pointer cast with incompatible types.
CFLAGS:append:harmony = " -Wno-implicit-function-declaration -Wno-int-conversion -Wno-incompatible-pointer-types"
CFLAGS:append:inharmony = " -Wno-implicit-function-declaration -Wno-int-conversion  -Wno-incompatible-pointer-types"

