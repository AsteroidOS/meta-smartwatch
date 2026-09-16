#!/bin/sh
# SPDX-License-Identifier: MIT
# Poll for ALSA card 0 to actually be registered (fixed sleeps race the real
# probe/enumeration time) before setting audio routing mixer controls.
# Bounded retry, not an infinite loop.
#
# numid=348: QUAT_MI2S_RX -> MultiMedia1 (speaker playback route)
# numid=1422: MultiMedia1 Mixer TERT_MI2S_TX (real mic capture backend -
#             the correct, non-dummy digital-codec-backed capture path,
#             NOT the QUAT_MI2S_TX dummy-codec route, and NOT the
#             hw:0,11 "Hostless" device, which is architecturally
#             incapable of host capture)
# numid=83:  DEC1 MUX -> DMIC1 (selects the real digital mic as DEC1's
#             input source)
#
# PulseAudio's own capture-direction device probing during startup resets
# the mic-specific controls (1422, 83) but never touches the speaker
# control (348, playback-only). Confirmed causally by masking
# pulseaudio.service entirely and observing the mic controls hold
# indefinitely with PA never running. Fix: wait for pulseaudio.service to
# actually be active plus a settle buffer, THEN set all mixer controls
# together at the end, so nothing is set-then-clobbered by PulseAudio's
# own probing.

i=0
while [ $i -lt 60 ]; do
	if [ -e /proc/asound/card0 ]; then
		sleep 0.5
		break
	fi
	i=$((i+1))
	sleep 0.5
done

if [ ! -e /proc/asound/card0 ]; then
	echo "audio-mixer-fix-hoki: TIMED OUT waiting for card 0" >> /tmp/audio-mixer-fix-hoki.log
	exit 1
fi
echo "audio-mixer-fix-hoki: card0 present after ${i} x 0.5s polls" >> /tmp/audio-mixer-fix-hoki.log

# Wait for pulseaudio.service (user session, ceres, uid 1000) to actually be
# active before touching any of the mixer controls, since its startup/probe
# resets them.
j=0
pa_active=0
while [ $j -lt 40 ]; do
	if su ceres -c 'systemctl --user is-active pulseaudio.service' 2>/dev/null | grep -q '^active$'; then
		pa_active=1
		break
	fi
	j=$((j+1))
	sleep 0.5
done
echo "audio-mixer-fix-hoki: pulseaudio active=${pa_active} after ${j} x 0.5s polls" >> /tmp/audio-mixer-fix-hoki.log

# Settle buffer: pulseaudio becoming "active" doesn't mean its own device
# probing has finished yet. Empirically generous, not timing-critical.
sleep 15

amixer -c 0 cset numid=348 1,1 >>/tmp/audio-mixer-fix-hoki.log 2>&1
amixer -c 0 cset numid=1422 1,1 >>/tmp/audio-mixer-fix-hoki.log 2>&1
amixer -c 0 cset numid=83 DMIC1 >>/tmp/audio-mixer-fix-hoki.log 2>&1
echo "audio-mixer-fix-hoki: all routes set (post-pulseaudio-settle)" >> /tmp/audio-mixer-fix-hoki.log

exit 0
