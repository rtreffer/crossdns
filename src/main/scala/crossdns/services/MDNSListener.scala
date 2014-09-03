package crossdns.services

import crossdns.model.ZoneSource
import java.net.NetworkInterface
import java.net.MulticastSocket
import java.net.InetAddress
import java.net.DatagramPacket
import de.measite.minidns.DNSMessage
import scala.collection.JavaConversions._
import crossdns.model.A
import crossdns.model.Record
import java.net.Inet4Address
import crossdns.model.AAAA
import java.net.Inet6Address
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.HashSet
import de.measite.minidns.record.PTR
import de.measite.minidns.record.SRV
import de.measite.minidns.record.CNAME
import de.measite.minidns.Question
import de.measite.minidns.Record.TYPE
import scala.util.Random
import de.measite.minidns.Record.CLASS
import java.io.IOException

/**
 * Listens on a given interface for ipv6 and ipv4 mdns traffic.
 */
class MDNSListener(val interfaceName : String) extends ZoneSource with Service {

    // alpha-diget-hyphen for hostnames
    val validName = "^[a-zA-Z0-9-][a-zA-Z0-9-]*$"

    val interface = NetworkInterface.getByName(interfaceName)

    val serviceName = "mdns(" + interfaceName + ")"

    val log = crossdns.Syslog

    val ms4 = new MulticastSocket(5353);
    ms4.setSoTimeout(100);
    ms4.setReuseAddress(true);
    ms4.setBroadcast(true);
    ms4.joinGroup(InetAddress.getByName("224.0.0.251"));
    ms4.setNetworkInterface(interface);

    val ms6 = new MulticastSocket(5353);
    ms6.setSoTimeout(100);
    ms6.setReuseAddress(true);
    ms6.setBroadcast(true);
    ms6.joinGroup(InetAddress.getByName("FF02::FB"));
    ms6.setNetworkInterface(interface);

    var lastNames = HashSet[String]()
    val rnd = new Random

    def baseSearch = {
        val question =
            List(
                new Question("_workstation._tcp.local", TYPE.PTR, CLASS.IN, true)
            )
        val msg = new DNSMessage
        msg.setQuestions(question :_*)
        msg.setId(rnd.nextInt)
        val dnsQueryPayload = msg.toArray()
        val dp = new DatagramPacket(dnsQueryPayload, dnsQueryPayload.length)
        dp.setPort(5353)
        dp.setAddress(InetAddress.getByName("224.0.0.251"))
        try { ms4.send(dp) } catch { case _ : IOException => }
        dp.setAddress(InetAddress.getByName("FF02::FB"))
        try { ms6.send(dp) } catch { case _ : IOException => }
    }

    def act() {
        val names = HashSet[String]()
        val in = new DatagramPacket(new Array[Byte](12 * 1024), 12 * 1024);
        var i = 0
        def handle(message : DNSMessage) : Unit = {
                val records =
                    message.getAdditionalResourceRecords() ++
                    message.getAnswers()
                records.foreach(record => {
                    val name = record.getName().split("[.]")(0)
                    if (name.matches(validName)) {
                        names.add(name)
                    }
                    record.getPayload() match {
                        case cname : CNAME => { // matches PTR, too
                            val name = cname.getName.split("[.]")(0)
                            if (name.matches(validName)) {
                                names.add(name)
                            }
                        }
                        case srv : SRV => {
                            val name = srv.getName.split("[.]")(0)
                            if (name.matches(validName)) {
                                names.add(name)
                            }
                        }
                        case _ => {}
                    }
                })
                val output = records.filter(record => {
                    record.getPayload().isInstanceOf[de.measite.minidns.record.A] ||
                    record.getPayload().isInstanceOf[de.measite.minidns.record.AAAA]
                })
                .map(record => (record.getName().split("[.]")(0),record))
                .filter(_._1.matches(validName))
                .map(e => {
                    val record = e._2
                    val name = e._1
                    record.getPayload() match {
                        case a : de.measite.minidns.record.A =>
                            Record[A](name, record.getTtl().toInt, A(
                                InetAddress.getByName(a.toString()).asInstanceOf[Inet4Address]
                            ))
                        case aaaa : de.measite.minidns.record.AAAA =>
                            Record[AAAA](name, record.getTtl().toInt, AAAA(
                                InetAddress.getByName(aaaa.toString()).asInstanceOf[Inet6Address]
                            ))
                    }
                })
                sinkLock.locked({
                    sinks.foreach(sink => {
                        output.foreach(r => {
                            // ttl is measured in seconds, expire in milliseconds
                            sink.add(r, r.ttl * 2l * 1000l)
                        })
                    })
                })
        }
        // run for one minute
        while (runService && i < 300) {
            i += 1
            try {
                ms4.receive(in)
                handle(DNSMessage.parse(in.getData()))
            } catch {
                case timeout : SocketTimeoutException => {}
                case t : Throwable => t.printStackTrace()
            }
            try {
                ms6.receive(in)
                handle(DNSMessage.parse(in.getData()))
            } catch {
                case timeout : SocketTimeoutException => {}
                case t : Throwable => t.printStackTrace()
            }
        }
        // now search for A/AAAA records by name
        (lastNames.clone ++ names.clone).grouped(10).map(group => {
            val question = group.map(name => {
                List(new Question(name + ".local", TYPE.A, CLASS.IN, true),
                     new Question(name + ".local", TYPE.AAAA, CLASS.IN, true))
            }).flatten.toList
            val msg = new DNSMessage
            msg.setQuestions(question :_*)
            msg.setId(rnd.nextInt)
            val dnsQueryPayload = msg.toArray()
            val dp = new DatagramPacket(dnsQueryPayload, dnsQueryPayload.length)
            dp.setPort(5353)
            dp.setAddress(InetAddress.getByName("224.0.0.251"))
            try { ms4.send(dp) } catch { case _ : IOException => }
            dp.setAddress(InetAddress.getByName("FF02::FB"))
            try { ms6.send(dp) } catch { case _ : IOException => }
        })
        lastNames = names
        if (rnd.nextDouble() < 0.2) {
            baseSearch
        }
    }

    override def start = {
        baseSearch
        super.start()
    }
}
