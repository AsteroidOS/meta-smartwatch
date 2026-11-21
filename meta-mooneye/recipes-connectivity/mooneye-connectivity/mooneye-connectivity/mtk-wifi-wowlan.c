/*
 * Enables the "any" wake-on-WLAN trigger on the wiphy behind a network
 * interface (what "iw phy <phy> wowlan enable any" does), or shows it.
 *
 * Without a WoWLAN configuration, cfg80211 disconnects from the network on
 * every suspend attempt. On the MT6630 that disconnect takes a wakelock and
 * aborts the suspend, so autosleep retries many times a second, the watch
 * never sleeps and Wi-Fi drops. With the trigger set, cfg80211 leaves the
 * connection alone and the chip stays associated while the host sleeps.
 *
 * Usage: mtk-wifi-wowlan [interface] [show]   (default interface: wlan0)
 *
 * SPDX-License-Identifier: Apache-2.0
 */
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/genetlink.h>
#include <linux/netlink.h>
#include <linux/nl80211.h>

#define BUF_SIZE 8192

struct msg {
    char buf[BUF_SIZE];
    size_t len;
};

static struct nlmsghdr *msg_init(struct msg *m, __u16 type, __u16 flags, __u8 cmd)
{
    struct nlmsghdr *nlh = (struct nlmsghdr *)m->buf;
    struct genlmsghdr *gh;

    memset(m->buf, 0, sizeof(m->buf));
    nlh->nlmsg_type = type;
    nlh->nlmsg_flags = NLM_F_REQUEST | flags;
    nlh->nlmsg_seq = 1;
    gh = NLMSG_DATA(nlh);
    gh->cmd = cmd;
    gh->version = 1;
    m->len = NLMSG_LENGTH(GENL_HDRLEN);
    return nlh;
}

static struct nlattr *attr_put(struct msg *m, __u16 type, const void *data, size_t len)
{
    struct nlattr *a = (struct nlattr *)(m->buf + NLMSG_ALIGN(m->len));

    a->nla_type = type;
    a->nla_len = NLA_HDRLEN + len;
    if (len)
        memcpy((char *)a + NLA_HDRLEN, data, len);
    m->len = NLMSG_ALIGN(m->len) + NLA_ALIGN(a->nla_len);
    return a;
}

static int transact(int fd, struct msg *m, char *reply, size_t reply_size)
{
    struct nlmsghdr *nlh = (struct nlmsghdr *)m->buf;
    ssize_t n;

    nlh->nlmsg_len = m->len;
    if (send(fd, m->buf, m->len, 0) < 0)
        return -errno;
    n = recv(fd, reply, reply_size, 0);
    if (n < 0)
        return -errno;
    nlh = (struct nlmsghdr *)reply;
    if (!NLMSG_OK(nlh, (size_t)n))
        return -EIO;
    if (nlh->nlmsg_type == NLMSG_ERROR)
        return ((struct nlmsgerr *)NLMSG_DATA(nlh))->error;
    return (int)n;
}

/* Walks the attributes after the genl header of the first message. */
static struct nlattr *find_attr(char *reply, __u16 type)
{
    struct nlmsghdr *nlh = (struct nlmsghdr *)reply;
    struct nlattr *a = (struct nlattr *)((char *)NLMSG_DATA(nlh) + GENL_HDRLEN);
    int rem = nlh->nlmsg_len - NLMSG_LENGTH(GENL_HDRLEN);

    while (rem >= NLA_HDRLEN && a->nla_len >= NLA_HDRLEN && a->nla_len <= rem) {
        if ((a->nla_type & NLA_TYPE_MASK) == type)
            return a;
        rem -= NLA_ALIGN(a->nla_len);
        a = (struct nlattr *)((char *)a + NLA_ALIGN(a->nla_len));
    }
    return NULL;
}

static int read_phy_index(const char *ifname)
{
    char path[128];
    FILE *f;
    int idx = -1;

    snprintf(path, sizeof(path), "/sys/class/net/%s/phy80211/index", ifname);
    f = fopen(path, "r");
    if (!f)
        return -1;
    if (fscanf(f, "%d", &idx) != 1)
        idx = -1;
    fclose(f);
    return idx;
}

int main(int argc, char **argv)
{
    const char *ifname = argc > 1 ? argv[1] : "wlan0";
    int show = argc > 2 && strcmp(argv[2], "show") == 0;
    static char reply[BUF_SIZE];
    struct sockaddr_nl sa = { .nl_family = AF_NETLINK };
    struct msg m;
    struct nlattr *a;
    __u16 family;
    __u32 wiphy;
    int fd, ret, idx;

    idx = read_phy_index(ifname);
    if (idx < 0) {
        fprintf(stderr, "mtk-wifi-wowlan: %s has no wiphy\n", ifname);
        return 1;
    }
    wiphy = idx;

    fd = socket(AF_NETLINK, SOCK_RAW, NETLINK_GENERIC);
    if (fd < 0 || bind(fd, (struct sockaddr *)&sa, sizeof(sa)) < 0) {
        perror("mtk-wifi-wowlan: netlink");
        return 1;
    }

    /* Resolve the nl80211 generic netlink family. */
    msg_init(&m, GENL_ID_CTRL, 0, CTRL_CMD_GETFAMILY);
    attr_put(&m, CTRL_ATTR_FAMILY_NAME, NL80211_GENL_NAME, strlen(NL80211_GENL_NAME) + 1);
    ret = transact(fd, &m, reply, sizeof(reply));
    if (ret < 0 || !(a = find_attr(reply, CTRL_ATTR_FAMILY_ID))) {
        fprintf(stderr, "mtk-wifi-wowlan: nl80211 not found (%d)\n", ret);
        return 1;
    }
    memcpy(&family, (char *)a + NLA_HDRLEN, sizeof(family));

    if (show) {
        msg_init(&m, family, 0, NL80211_CMD_GET_WOWLAN);
        attr_put(&m, NL80211_ATTR_WIPHY, &wiphy, sizeof(wiphy));
        ret = transact(fd, &m, reply, sizeof(reply));
        if (ret < 0) {
            fprintf(stderr, "mtk-wifi-wowlan: get failed: %s\n", strerror(-ret));
            return 1;
        }
        printf("phy%u wowlan: %s\n", wiphy,
               find_attr(reply, NL80211_ATTR_WOWLAN_TRIGGERS) ? "enabled" : "disabled");
        return 0;
    }

    /* SET_WOWLAN with a nested trigger list holding the ANY flag. */
    msg_init(&m, family, NLM_F_ACK, NL80211_CMD_SET_WOWLAN);
    attr_put(&m, NL80211_ATTR_WIPHY, &wiphy, sizeof(wiphy));
    a = attr_put(&m, NL80211_ATTR_WOWLAN_TRIGGERS | NLA_F_NESTED, NULL, 0);
    attr_put(&m, NL80211_WOWLAN_TRIG_ANY, NULL, 0);
    a->nla_len = (char *)m.buf + m.len - (char *)a;
    ret = transact(fd, &m, reply, sizeof(reply));
    if (ret < 0) {
        fprintf(stderr, "mtk-wifi-wowlan: set failed: %s\n", strerror(-ret));
        return 1;
    }
    printf("phy%u wowlan: any\n", wiphy);
    close(fd);
    return 0;
}
