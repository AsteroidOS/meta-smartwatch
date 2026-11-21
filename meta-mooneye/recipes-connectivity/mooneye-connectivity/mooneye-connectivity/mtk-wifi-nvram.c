/*
 * mtk-wifi-nvram: hand the per-unit Wi-Fi NVRAM record (MAC address, TX power
 * calibration, country code, ...) to the MediaTek gen3 WLAN driver.
 *
 * On Android, wlan_assistant does this through the WIFI_IOCTL_SET_NVRAM ioctl
 * on /dev/wmtWifi (see drivers/misc/mediatek/connectivity/wlan/wmt_chrdev_wifi.c).
 * The driver copies 512 bytes and uses them the next time the WLAN function is
 * powered on (glLoadNvram). Without it, the driver generates a new random MAC
 * address on every power-on.
 *
 * Usage: mtk-wifi-nvram [/data/nvram/APCFG/APRDEB/WIFI]
 */
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

#define WIFI_IOCTL_MAGIC     'k'
#define WIFI_IOCTL_SET_NVRAM _IOW(WIFI_IOCTL_MAGIC, 0x1a, int)
#define NVRAM_SIZE           512

int main(int argc, char **argv)
{
	const char *path = argc > 1 ? argv[1] : "/data/nvram/APCFG/APRDEB/WIFI";
	unsigned char buf[NVRAM_SIZE];
	ssize_t got = 0;
	int fd;

	fd = open(path, O_RDONLY);
	if (fd < 0) {
		fprintf(stderr, "mtk-wifi-nvram: %s: %s\n", path, strerror(errno));
		return 1;
	}
	while (got < NVRAM_SIZE) {
		ssize_t n = read(fd, buf + got, NVRAM_SIZE - got);
		if (n <= 0)
			break;
		got += n;
	}
	close(fd);
	if (got != NVRAM_SIZE) {
		fprintf(stderr, "mtk-wifi-nvram: %s: expected at least %d bytes\n", path, NVRAM_SIZE);
		return 1;
	}

	fd = open("/dev/wmtWifi", O_RDWR);
	if (fd < 0) {
		fprintf(stderr, "mtk-wifi-nvram: /dev/wmtWifi: %s\n", strerror(errno));
		return 1;
	}
	if (ioctl(fd, WIFI_IOCTL_SET_NVRAM, buf) != 0) {
		fprintf(stderr, "mtk-wifi-nvram: WIFI_IOCTL_SET_NVRAM failed: %s\n", strerror(errno));
		close(fd);
		return 1;
	}
	close(fd);
	printf("mtk-wifi-nvram: NVRAM handed to the driver (MAC %02x:%02x:%02x:%02x:%02x:%02x)\n",
	       buf[4], buf[5], buf[6], buf[7], buf[8], buf[9]);
	return 0;
}
