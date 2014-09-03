package crossdns.services

import jcifs.smb.SmbFile
import jcifs.smb.NtlmPasswordAuthentication
import scala.collection.JavaConversions._
import jcifs.UniAddress
import jcifs.netbios.Lmhosts
import scala.collection.mutable.{HashMap}
import crossdns.model._
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import jcifs.netbios.NbtAddress
import java.net.NetworkInterface

class SMBBrowser extends ZoneSource with Service {

    val serviceName = "smb"

    val log = crossdns.Syslog

    jcifs.Config.registerSmbURLHandler()
    jcifs.Config.setProperty("jcifs.smb.client.username", "Guest")
    jcifs.Config.setProperty("jcifs.smb.client.password", "")

    def act() {
        try {
        val smbFile = new SmbFile("smb://", NtlmPasswordAuthentication.ANONYMOUS)
        smbFile.listFiles.map(
                domain => try {
                    Thread.sleep(1)
                    domain.listFiles.map(
                    server => try {
                        Thread.sleep(1)
                        val host = server.getServer.toLowerCase
                        log.i("Discovered  + host")
                        val record =
                            UniAddress.getAllByName(host, true).map(_.getAddress).toList.
                            map {
                                case e : NbtAddress => e.getInetAddress
                                case x => x
                            }.
                            filter(_.isInstanceOf[InetAddress]).map(_.asInstanceOf[InetAddress]).
                            filter(!_.isLoopbackAddress).map(e => {
                                if (e.isInstanceOf[Inet4Address]) {
                                    val r = Record(host, 60, A(e.asInstanceOf[Inet4Address]))
                                    log.d("Discovered " + r)
                                    r
                                } else {
                                    val r = Record(host, 60, AAAA(e.asInstanceOf[Inet6Address]))
                                    log.d("Discovered " + r)
                                    r
                                }
                            })
                        sinkLock.locked {
                            sinks.foreach(sink => {
                                record.foreach(sink.add(_, 5 * 60 * 1000))
                            })
                        }
                    } catch {case e : Throwable => List()}
                ) } catch {case e : Throwable => ()}
            )
	} catch {case e : Throwable => {}}
        Thread.sleep(60000)
    }

}
