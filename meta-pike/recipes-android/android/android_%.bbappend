do_install:append:pike() {
    cd ${D}
    ln -s system/vendor vendor
}

FILES:android-system:append:pike = " /vendor"
