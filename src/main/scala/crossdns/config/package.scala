package crossdns

import crossdns.model.Payload

package object config {

    val log = crossdns.Syslog
    type Zone = crossdns.model.Zone
    type NSUpdate = crossdns.services.NSUpdate
    type MDNS = crossdns.services.MDNSListener
    type SMB = crossdns.services.SMBBrowser
    type Record[T <: Payload] = crossdns.model.Record[T]
    type Payload = crossdns.model.Payload
    type AAAA = crossdns.model.AAAA
    type A = crossdns.model.A
    type WebServer = crossdns.web.WebServer

    object Zone {
        def apply(name : String) = new Zone(name)
    }
    object SMB {
        def apply() = new SMB()
    }
    object MDNS {
        def apply(interface : String) = new MDNS(interface)
    }
    object NSUpdate {
        def apply(
            server : Option[String] = None,
            zone : Option[String] = None,
            key : Option[String] = None,
            keyname : Option[String] = None,
            ttl : Long = 60,
            updateInterval : Long = 60000,
            updateTimeout : Long = 60000
        ) = new NSUpdate(server,zone,key,keyname,ttl,updateInterval,updateTimeout)
    }
    object WebServer {
        def apply(port : Int) = new WebServer(port)
    }

    def ipv6PrivacyEnabled(record : Record[Payload]) = {
        record.payload match {
            case aaaa: AAAA =>
                ({val b = aaaa.ip.getAddress(); b(11)} & 0xff) != 0xff ||
                ({val b = aaaa.ip.getAddress(); b(12)} & 0xfe) != 0xfe
            case _ => false
        }
    }

    def ipv6WorldRoutable[T <: Payload](record : Record[T]) = {
        record.payload match {
            case aaaa: AAAA =>
                ({val b = aaaa.ip.getAddress(); b(0)} & 0xe0) == 0x20
            case _ => false
        }
    }

    def `private`(record : Record[Payload]) = {
        record.payload match {
            case a: A =>
                a.ip.isAnyLocalAddress() || a.ip.isLinkLocalAddress() ||
                a.ip.isLoopbackAddress() || a.ip.isSiteLocalAddress()
            case aaaa: AAAA =>
                !ipv6WorldRoutable(record) || ipv6PrivacyEnabled(record)
            case _ => true
        }
    }

    def not[T <: Payload](f : Record[T] => Boolean) : Record[T] => Boolean = {
        def g(record : Record[T]) = {
            !f(record)
        }
        g(_)
    }

   def ![T <: Payload](f : Record[T] => Boolean) = not(f)

}
