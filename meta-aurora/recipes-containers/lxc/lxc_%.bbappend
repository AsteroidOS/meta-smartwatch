# lxc pulls dnsmasq in for lxc-net, the bridge it can set up for containers
# with their own network namespace. The Android container shares the host's
# network namespace (lxc.namespace.keep = net), so lxc-net is never used, and
# dnsmasq.service only failed at every boot because connman already serves
# DNS on port 53.
RDEPENDS:${PN}:remove = "dnsmasq"
