// SPDX-License-Identifier: GPL-2.0-only
/* wl-privcmd: send an Android DHD private command (e.g. "SETSUSPENDMODE 1")
 * to a wlan interface via SIOCDEVPRIVATE+1, the path the Android framework
 * uses at screen-off/on. Usage: wl-privcmd <iface> "<command>" */
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <net/if.h>
#include <linux/sockios.h>

struct priv_cmd { char *buf; int used_len; int total_len; };

int main(int argc, char **argv)
{
	char buf[256];
	struct priv_cmd pc;
	struct ifreq ifr;
	int fd, ret;

	if (argc != 3) { fprintf(stderr, "usage: %s <iface> \"<command>\"\n", argv[0]); return 2; }
	memset(buf, 0, sizeof(buf));
	strncpy(buf, argv[2], sizeof(buf) - 1);
	pc.buf = buf;
	pc.used_len = strlen(buf) + 1;
	pc.total_len = sizeof(buf);
	memset(&ifr, 0, sizeof(ifr));
	strncpy(ifr.ifr_name, argv[1], IFNAMSIZ - 1);
	ifr.ifr_data = (void *)&pc;
	fd = socket(AF_INET, SOCK_DGRAM, 0);
	if (fd < 0) { perror("socket"); return 1; }
	ret = ioctl(fd, SIOCDEVPRIVATE + 1, &ifr);
	if (ret < 0) { perror("ioctl"); close(fd); return 1; }
	printf("%s: ok (%d) %s\n", argv[2], ret, buf);
	close(fd);
	return 0;
}
