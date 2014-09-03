package crossdns

import com.sun.jna.Library
import com.sun.jna.Native
import java.util.concurrent.ArrayBlockingQueue

trait SyslogLibrary extends Library {
    def openlog (ident : String, option : Int, facility : Int) : Unit
    def syslog (priority : Int, message : String) : Unit
    def closelog () : Unit
}

object Syslog {
// Priorities
    val LOG_EMERG   = 0
    val LOG_ALERT   = 1
    val LOG_CRIT    = 2
    val LOG_ERR     = 3
    val LOG_WARNING = 4
    val LOG_NOTICE  = 5
    val LOG_INFO    = 6
    val LOG_DEBUG   = 7
// Facilities
    val LOG_KERN     = (0<<3)
    val LOG_USER     = (1<<3)
    val LOG_MAIL     = (2<<3)
    val LOG_DAEMON   = (3<<3)
    val LOG_AUTH     = (4<<3)
    val LOG_SYSLOG   = (5<<3)
    val LOG_LPR      = (6<<3)
    val LOG_NEWS     = (7<<3)
    val LOG_UUCP     = (8<<3)
    val LOG_CRON     = (9<<3)
    val LOG_AUTHPRIV = (10<<3)
    val LOG_FTP      = (11<<3)
    val LOG_LOCAL0   = (16<<3)
    val LOG_LOCAL1   = (17<<3)
    val LOG_LOCAL2   = (18<<3)
    val LOG_LOCAL3   = (19<<3)
    val LOG_LOCAL4   = (20<<3)
    val LOG_LOCAL5   = (21<<3)
    val LOG_LOCAL6   = (22<<3)
    val LOG_LOCAL7   = (23<<3)
// Options
    val LOG_PID    = 0x01
    val LOG_CONS   = 0x02
    val LOG_ODELAY = 0x04
    val LOG_NDELAY = 0x08
    val LOG_NOWAIT = 0x10
    val LOG_PERROR = 0x20

    val instance = Native.loadLibrary("c",classOf[SyslogLibrary]).asInstanceOf[SyslogLibrary]

    def syslog(level : Int, message : String) = instance.synchronized({
        instance.openlog("crossdns", LOG_NDELAY | LOG_PERROR, LOG_DAEMON)
        instance.syslog(level, message.replaceAll("%", "%%"))
        instance.closelog()
    })

    def emerg(message : String)   = syslog(LOG_EMERG, message)
    def alert(message : String)   = syslog(LOG_ALERT, message)
    def a(message : String)       = syslog(LOG_ALERT, message)
    def crit(message : String)    = syslog(LOG_CRIT, message)
    def c(message : String)       = syslog(LOG_CRIT, message)
    def err(message : String)     = syslog(LOG_ERR, message)
    def e(message : String)       = syslog(LOG_ERR, message)
    def warning(message : String) = syslog(LOG_WARNING, message)
    def w(message : String)       = syslog(LOG_WARNING, message)
    def notice(message : String)  = syslog(LOG_NOTICE, message)
    def n(message : String)       = syslog(LOG_NOTICE, message)
    def info(message : String)    = syslog(LOG_INFO, message)
    def i(message : String)       = syslog(LOG_INFO, message)
    def debug(message : String)   = syslog(LOG_DEBUG, message)
    def d(message : String)       = syslog(LOG_DEBUG, message)
}
