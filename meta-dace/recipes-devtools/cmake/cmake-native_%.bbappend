# cmake 3.31.x (oe-core walnascar) bootstrap fails to link on hosts with
# GCC >= 12 (Arch Linux: GCC 16):
#
#   undefined reference to `cmGlobalGhsMultiGenerator::...'
#
# Root cause: with -O2 the compiler instantiates the GHS generator factory
# (declared in cmake.cxx via cmGlobalGhsMultiGenerator.h) even though the
# bootstrap build does not compile the GHS generator sources. This is an
# upstream cmake bug (the GHS sources are only added to the *full* build's
# CMakeLists.txt, not to the bootstrap script's hardcoded source list).
#
# Workaround: build cmake-native without -O2, which avoids the spurious
# template instantiation. Only affects cmake-native build speed slightly.
CXXFLAGS:remove = "-O2"
CFLAGS:remove = "-O2"