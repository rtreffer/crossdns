CrossDNS - crossing the line between autodiscovery and dns
==========================================================

Why CrossDNS?
-------------

I have IPv6 at home. And I love avahi. I thus wanted a tool that would take
all local machines and publish their AAAA records to my personal dns zone.

This means that I can add a public service on my lan by starting a lxc
container and adding one firewall rule. No fuzzing around with local dns
update scripts. No problems with changing prefixes. Simple and fast.

What con CrossDNS do for you?
-----------------------------

CrossDNS builds virtual zones that
- can be filled by discovery services (currently mdns and smb)
- can be filtered (e.g. all addresses that are "not private")
- can be uploaded by export tools (nsupdate)

CrossDNS is configured via one scala file that is compiled on startup.

Logging is done via syslog. The config is read from
- The current working directory (for testing)
- /etc (/etc/crossdns.conf)

CrossDNS runs well on low end hardware (Raspberry Pi B, 256MB RAM version
is a reasonable crossdns server. CrossDNS needs ~128MB RAM on that machine
given java 8 as a runtime but startup will take 2 minutes due to the scala
compiler and a cold jvm).

Licence
=======

CrossDNS is licenced under the terms of the Apache Licence, Version 2.

Ideas
=====

Here are some ideas if you'd like to contribute (or lack one of those)
- Web UI (unfiltered can be started, with zones, some json endpoints work)
- Web Zones (curl to add/remove/update records? check!)
- scan dhcp+dns server queries (home network with 256 IPs? Scan!)
- Native DNS server functionality (to be used as a delegate zone on a full recursor)
- Zone transfer (as input and output)
- lldp (if possible)
- pcap + arp + dns lookups

There are many many ways to find hosts on your network. Find one that works
for you and open a pull request.
