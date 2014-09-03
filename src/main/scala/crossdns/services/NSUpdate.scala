package crossdns.services

import scala.sys.process._
import java.io.ByteArrayInputStream
import crossdns.model.Zone
import crossdns.Syslog
import crossdns.model._

class NSUpdate(
    server : Option[String] = None,
    zone : Option[String] = None,
    key : Option[String] = None,
    keyname : Option[String] = None,
    ttl : Long = 60,
    updateInterval : Long = 60000,
    updateTimeout : Long = 60000
) extends Zone(zone.getOrElse("")) with Service {

    val serviceName = "nsupdate"

    def act() : Unit = {
        Thread.sleep(60000)

        val records = getAll()

        val delete = records.map(record => record.payload match {
            case aaaa : AAAA => "update delete " + record.name + "." + zone.get + ". AAAA\n"
            case a : A       => "update delete " + record.name + "." + zone.get + ". A\n"
        }).toSet.toList.mkString
        val add = records.map(record => record.payload match {
            case aaaa : AAAA => s"update add " + record.name + "." + zone.get + s". ${ttl} AAAA ${aaaa.ip.getHostAddress()}\n"
            case a : A       => s"update add " + record.name + "." + zone.get + s". ${ttl} A ${a.ip.getHostAddress()}\n"
        }).toSet.toList.mkString

        val command =
            // setup
            server.map("server " + _ + "\n").getOrElse("") +
            zone.map("zone " + _ + "\n").getOrElse("") +
            key.map("key " + keyname.getOrElse("_key") +  " " + _ + "\n").getOrElse("") +
            delete +
            add +
            "send\nquit\n"

        log.i(command)
        (Seq("nsupdate", "-t", s"${updateTimeout / 1000l}") #< new ByteArrayInputStream(command.getBytes)).!
    }

}
