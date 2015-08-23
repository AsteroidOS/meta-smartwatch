DESCRIPTION = "Some qualcomm devices need a refresher daemon to show something on screen. This is a simple one."
PR = "r0"
SRC_URI = "git://github.com/Asteroid-Project/msm-fb-refresher.git;protocol=https"
SRCREV = "dae95fa2cb96c7b470916034c32f5f80eb122dc8"
LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://refresher.c;beginline=1;endline=16;md5=d43f405814d21fdfafe4466940d1ba68"
S = "${WORKDIR}/git/"
PACKAGE_DEBUG_SPLIT_STYLE = "debug-without-src"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} ${S}/refresher.c -o ${S}/msm-fb-refresher
}

do_install() {
    install -m 0755 -d ${D}${bindir}
    install -m 0755 ${S}/msm-fb-refresher ${D}${bindir}
}
