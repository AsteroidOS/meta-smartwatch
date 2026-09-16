# HOKI FIX: this device's 2017-era kernel UAPI header predates ALSA's Y2038
# time64 ABI transition (struct snd_pcm_sync_ptr etc.). Modern OE toolchains
# define _TIME_BITS=64 by default, causing alsa-lib to compute a LARGER
# struct snd_pcm_sync_ptr (136 bytes) than this kernel's own header expects
# (132 bytes) -- the resulting SNDRV_PCM_IOCTL_SYNC_PTR ioctl numbers differ
# (0xc0884123 vs 0xc0844123), so the kernel silently rejects every PCM
# ioctl call from aplay/PulseAudio with -ENOTTY, since it never matches any
# case in its switch statement. Undefine _TIME_BITS to fall back to the
# legacy 32-bit time_t struct layout matching this kernel's own headers.
CFLAGS:append = " -U_TIME_BITS"
