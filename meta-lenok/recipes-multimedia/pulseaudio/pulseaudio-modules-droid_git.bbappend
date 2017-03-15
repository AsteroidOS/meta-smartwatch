SRCREV_lenok = "e0c325217f2c5674cfbc4b8c1230eadacd2c5784"

do_configure_prepend() {
	touch src/common/droid-util-51.h
}
