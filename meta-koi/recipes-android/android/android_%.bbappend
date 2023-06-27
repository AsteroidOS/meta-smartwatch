do_install:append:koi() {
    cd ${D}
    ln -s system/vendor vendor
}

FILES:android-system:append:koi = " /vendor"
