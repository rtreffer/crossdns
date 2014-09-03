package crossdns.model

import java.net.Inet4Address
import java.net.Inet6Address

case class Record[+T <: Payload](name : String, ttl : Int, payload : T)
sealed trait Payload
case class A(ip : Inet4Address) extends Payload
case class AAAA(ip : Inet6Address) extends Payload
