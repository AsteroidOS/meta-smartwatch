#!/usr/bin/perl
# Powers on / cycles the BT chip power via /dev/btpower
# (BT_CMD_PWR_CTRL = 0xbfad; the argument is the mode: 0=off, 1=on, 2=retention).
#
# WHY IT IS NEEDED: aurora (Pixel Watch 2) powers up the chip via /dev/btpower.
# On our watch the Qualcomm HAL, with soc=slate, does NOT vote regulators
# ("FOR SLATE not voting any Regulators") and without the supplies in the DT
# `btpower` also does not know which rails to touch: measured 2026-09-20,
# pm5100_l13 (core 1.304V) and pm5100_l17 (IO 1.8V) stay at state=disabled and
# the chip is MUTE (the HAL opens ttyHS0 at 2400 bps and dies with InitTimeOut +
# err 0x55).
#
#   perl dace-bt-power.pl 1       -> turn on
#   perl dace-bt-power.pl 0       -> turn off (cuts the rails)
#   perl dace-bt-power.pl cycle   -> 0 -> 1: off and on, which also
#                                    RESETS the chip (restarts at 2400 bps)
use strict; use warnings;
my $BT_CMD_PWR_CTRL = 0xbfad;
my $mode = shift // 1;
sysopen(my $fh, "/dev/btpower", 2) or die "open /dev/btpower: $!\n";
if ($mode eq "cycle") {
    ioctl($fh, $BT_CMD_PWR_CTRL, 0);
    select(undef, undef, undef, 0.5);
    ioctl($fh, $BT_CMD_PWR_CTRL, 1);
    print "BT chip power cycle (0 -> 1)\n";
} else {
    ioctl($fh, $BT_CMD_PWR_CTRL, $mode) or die "ioctl: $!\n";
    print "BT_CMD_PWR_CTRL pwr=$mode\n";
}
close($fh);
