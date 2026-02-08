FILESEXTRAPATHS:prepend:rubyfish := "${THISDIR}/ngfd:"

RDEPENDS:${PN}:append:rubyfish = " ngfd-plugin-droid-vibrator "
