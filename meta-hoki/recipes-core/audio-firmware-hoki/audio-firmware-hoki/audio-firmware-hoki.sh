#!/bin/sh
# SPDX-License-Identifier: MIT
# Load the audio_analog_cdc / audio_digital_cdc (legacy variant) codec modules
# before audio_machine, which needs their exported msm_anlg_cdc_*/msm_digcdc_*
# symbols. See https://github.com/AsteroidOS/meta-smartwatch (audio bring-up).

modprobe audio_analog_cdc
modprobe audio_digital_cdc
modprobe audio_machine
