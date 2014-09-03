package crossdns

import java.net.Inet6Address
import java.net.InetAddress
import java.net.Inet4Address

package object util {

    def ipv6PrivacyEnabled(addr : Inet6Address) = {
        ({val b = addr.getAddress(); b(11)} & 0xff) != 0xff ||
        ({val b = addr.getAddress(); b(12)} & 0xfe) != 0xfe
    }

    def ipv6WorldRoutable(addr : Inet6Address) = {
        ({val b = addr.getAddress(); b(0)} & 0xe0) == 0x20
    }

    def isPrivate(addr : InetAddress) = {
        addr match {
            case v4 : Inet4Address =>
                v4.isAnyLocalAddress() || v4.isLinkLocalAddress() ||
                v4.isLoopbackAddress() || v4.isSiteLocalAddress()
            case v6 : Inet6Address =>
                !ipv6WorldRoutable(v6) || ipv6PrivacyEnabled(v6)
            case _ => true
        }
    }

}
