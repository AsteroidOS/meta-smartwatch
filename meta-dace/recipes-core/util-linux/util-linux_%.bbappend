# util-linux 2.40.4: fix build on hosts with glibc >= 2.41 + GCC 16.
#
# glibc turns bsearch() into a C23 function-like macro when _ISOC23_SOURCE
# is enabled, which features.h does UNCONDITIONALLY when _GNU_SOURCE is
# active (util-linux's config.h sets _GNU_SOURCE). lsfd.c passed a compound
# literal with an inner trailing comma to bsearch(); the preprocessor splits
# macro arguments on commas at nesting level 0 (braces don't protect them):
#
#   misc-utils/lsfd.c:1573: error: macro 'bsearch' passed 6 arguments, but takes just 5
#
# NOTE: -std=gnu17 does NOT help; the macro is unconditional under _GNU_SOURCE.
# The upstream fix wraps the compound literal in parentheses.
SRC_URI += "file://0001-lsfd-parenthesize-compound-literal-passed-to-bsearch.patch"
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"