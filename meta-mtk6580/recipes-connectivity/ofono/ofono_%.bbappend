FILESEXTRAPATHS_prepend_harmony := "${THISDIR}/ofono:"
SRC_URI_append_harmony = " file://mtk-rild.conf"

do_install_append_harmony() {
    install -d ${D}/var/lib/environment/ofono/
    cp ${WORKDIR}/mtk-rild.conf ${D}/var/lib/environment/ofono/
}
