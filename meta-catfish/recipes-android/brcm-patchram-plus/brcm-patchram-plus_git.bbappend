FILESEXTRAPATHS_prepend := "${THISDIR}/brcm-patchram-plus:"
SRC_URI += " file://patchram.service \
             file://patchram.sh "

do_install_append() {
    install -d ${D}/lib/systemd/system/multi-user.target.wants/
    cp ${WORKDIR}/patchram.service ${D}/lib/systemd/system/
    ln -s ../patchram.service ${D}/lib/systemd/system/multi-user.target.wants/patchram.service

    install -d ${D}/usr/bin/
    cp ${WORKDIR}/patchram.sh ${D}/usr/bin/
}

FILES_${PN} += "/lib/systemd/system/"
DEPENDS += "rfkill"
