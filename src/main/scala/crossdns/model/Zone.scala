package crossdns.model

import scala.collection._
import crossdns.util.Lock

trait ZoneView {
    def get[T <: Payload](name : String) : List[Record[T]]
}

trait ZoneSink {
    def add[T <: Payload](record : Record[T], autoexpire : Long = 3600 * 1000) : Unit = {}
}

trait ZoneSource {
    protected val sinkLock = new Lock
    protected var sinks = immutable.List[ZoneSink]()

    def addSink [T <: ZoneSink](sink : T) : T = {
        sinkLock.locked {
            sinks = sink :: sinks
        }
        sink
    }

    def -> [T <: ZoneSink](sink : T) : T = addSink(sink)
}

class Zone(val name : String) extends ZoneView with ZoneSink with ZoneSource {

    val log = crossdns.Syslog

    protected final case class ExpiringRecord(expires : Long, record : Record[Payload])

    protected val recordLock = new Lock
    protected var records = immutable.List[ExpiringRecord]()

    def getAll() : List[Record[Payload]] = {
        var result = List[Record[Payload]]()
        recordLock.locked {
            val now = System.currentTimeMillis()
            records = records.filter(_.expires >= now)
            result = records.map(_.record)
        }
        result
    }

    override def get[T <: Payload](name : String) : List[Record[T]] =
        getAll
            .filter(record =>
                record.name == name &&
                record.payload.isInstanceOf[Record[T]])
            .map(_.asInstanceOf[Record[T]])

    override def add[T <: Payload](record : Record[T], autoexpire : Long = 3600 * 1000) : Unit = {
        recordLock.locked {
            log.d("Add " + record + " to \"" + name + "\"")
            val now = System.currentTimeMillis()
            val expire = now + autoexpire
            records = ExpiringRecord(expire, record) :: records.filter(_.expires >= now)
        }

        sinks.foreach(_.add(record, autoexpire))
    }

    override def addSink [T <: ZoneSink](sink : T) : T = {
        sinkLock.locked {
            sinks = sink :: sinks
            // replay
            recordLock.locked {
                val now = System.currentTimeMillis()
                records = records.filter(_.expires >= now)
                records.foreach(record => sink.add(record.record, record.expires - now))
            }
        }
        sink
    }

    def filter(f : FilterZone) : FilterZone = addSink(f)
    def filter(f : (Record[Payload], Long) => Boolean) : FilterZone =
        filter(new FilterZone(this.name, f))
    def filter(f : Record[Payload] => Boolean) : FilterZone =
        filter(new SimpleFilterZone(this.name, f))

}

class FilterZone(
    override val name : String,
    private val acceptFilter : (Record[Payload], Long) => Boolean
) extends Zone(name) {
    override def add[T <: Payload](record : Record[T], autoexpire : Long) : Unit =
        if (acceptFilter(record, autoexpire)) {
            super.add(record, autoexpire)
        }
}

class SimpleFilterZone(
    override val name : String,
    private val _acceptFilter : Record[Payload] => Boolean
) extends FilterZone(name, (r,_) => _acceptFilter(r)) {
}
