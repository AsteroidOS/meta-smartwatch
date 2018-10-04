FILESEXTRAPATHS_prepend_harmony := "${THISDIR}/libhybris:"
SRC_URI_harmony = "git://github.com/mer-hybris/libhybris;branch=master;protocol=https;branch=mm64-upstream \
                   file://0001-hooks-Removes-a-couple-of-problematic-hooks.patch \
                   file://0001-Fix-failure-to-build-with-glibc-2.26.patch"
SRCREV_harmony = "8251486bcdde95a6a78b5792fa663d8cda277cff"

FILESEXTRAPATHS_prepend_inharmony := "${THISDIR}/libhybris:"
SRC_URI_inharmony = "git://github.com/mer-hybris/libhybris;branch=master;protocol=https;branch=mm64-upstream \
                     file://0001-hooks-Removes-a-couple-of-problematic-hooks.patch \
                     file://0001-Fix-failure-to-build-with-glibc-2.26.patch"
SRCREV_inharmony = "8251486bcdde95a6a78b5792fa663d8cda277cff"
