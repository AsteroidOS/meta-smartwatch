FILESEXTRAPATHS:prepend:harmony := "${THISDIR}/ofono:"
SRC_URI:append:harmony = " file://mtk-rild.conf"

do_install:append:harmony() {
    install -d ${D}/var/lib/environment/ofono/
    cp ${WORKDIR}/mtk-rild.conf ${D}/var/lib/environment/ofono/
}

FILESEXTRAPATHS:prepend:inharmony := "${THISDIR}/ofono:"
SRC_URI:append:inharmony = " file://mtk-rild.conf"

do_install:append:inharmony() {
    install -d ${D}/var/lib/environment/ofono/
    cp ${WORKDIR}/mtk-rild.conf ${D}/var/lib/environment/ofono/
}
