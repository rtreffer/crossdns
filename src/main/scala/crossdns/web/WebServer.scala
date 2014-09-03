package crossdns.web

import unfiltered.request._
import unfiltered.response._
import unfiltered.response.Json._
import unfiltered.response.JsonContent._
import unfiltered.netty.Server
import unfiltered.netty.cycle.Plan
import unfiltered.netty.cycle.SynchronousExecution
import unfiltered.netty.ServerErrorResponse
import unfiltered.netty.resources.Resource
import dispatch.classic.Handler
import io.netty.handler.codec.http.HttpResponse
import scala.collection.mutable.WeakHashMap
import scala.collection.JavaConversions._
import java.io.File
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import crossdns.model.Zone
import crossdns.model.A
import crossdns.model.AAAA

@io.netty.channel.ChannelHandler.Sharable
class WebServer(val port : Int) extends Plan with ServerErrorResponse with SynchronousExecution {

    val log = crossdns.Syslog
    val server = Server.http(port).handler(this)

    val resources = new WeakHashMap[String, Array[Byte]]

    val zones = new ConcurrentHashMap[String, Zone]()

    log.i("Starting webserver on :" + port)
    server.start

    def add(zone : Zone) : WebServer = add(zone, zone.name)

    def add(zone : Zone, name : String) : WebServer = {
        if (zones.put(name, zone) != null) {
            // previous zone returned
            log.w("Zone " + name + " was redefined!")
        }
        this
    }

    def intent: unfiltered.netty.cycle.Plan.Intent = {
        case req @ GET(Path("/")) =>
            view("public/index.html")
        case req @ GET(Path(Seg("static" :: _))) =>
            view("public/" + req.uri.substring("static/".length))
        case req @ GET(Path(Seg("zone" :: Nil))) =>
            val json = zones.keySet().toList
            Json(json)
        case req @ GET(Path(Seg("zone" :: zone :: Nil))) =>
            val z = zones.get(zone)
            if (z == null) {
                Pass
            } else {
                val records = z.getAll.map(record =>
                    record.payload match {
                            case a : A => ("ttl" -> record.ttl) ~ ("name" -> record.name) ~ ("ip" -> a.ip.getHostAddress())
                            case aaaa : AAAA => ("ttl" -> record.ttl) ~ ("name" -> record.name) ~ ("ip" -> aaaa.ip.getHostAddress())
                            case _ => ("ttl" -> record.ttl) ~ ("name" -> record.name)
                     })
                val json =
                    ("zone" ->
                    ("name" -> zone) ~
                    ("records" -> records))
                Json(json)
            }
    }

    def view(fileName : String) : ResponseFunction[HttpResponse] = {
        val data = resources.get(fileName)
        if (data.isDefined) {
            ResponseBytes(data.get)
        } else try {
            log.d("Loading " + fileName)
            val file = new File(fileName)
            if (file.exists()) {
                resources.synchronized({
                    // make sure we only load one resource at a time
                    val buffer = new Array[Byte](file.length().toInt)
                    val in = new DataInputStream(new FileInputStream(file))
                    in.readFully(buffer)
                    in.close()
                    resources.put(fileName, buffer)
                    ResponseBytes(buffer)
                })
            } else {
                val url = getClass.getResource(fileName)
                resources.synchronized({
                    val buf = new Array[Byte](4 * 1024)
                    val in = getClass.getResourceAsStream(fileName)
                    val out = new ByteArrayOutputStream
                    var count = in.read(buf)
                    var b = -1
                    while (count > 0 || {b = in.read(); b} != -1) {
                        if (count == 0) {
                            out.write(b)
                        } else {
                            out.write(buf, 0, count)
                        }
                        count = in.read(buf)
                    }
                    in.close()
                    out.close()
                    val buffer = out.toByteArray()
                    resources.put(fileName, buffer)
                    ResponseBytes(buffer)
                })
            }
        } catch {
            case _ : FileNotFoundException => NotFound
            case _ : Throwable => InternalServerError
        }
    }

}
