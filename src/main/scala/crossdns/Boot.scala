package crossdns

import java.security.Security
import crossdns.services.NSUpdate
import java.io.File
import javax.script.ScriptEngineManager
import java.io.FileReader
import java.net.URLClassLoader
import java.net.URL

object Boot {

    def main(args: Array[String]) {
        Syslog.i("starting")

        val config =
            List("./crossdns.conf",
                 "/etc/crossdns.conf",
                 "/etc/crossdns/crossdns.conf"
            ).map(name => new File(name)).
	    filter(_.exists).filter(_.isFile).headOption

        if (config.isDefined) {
            configure(config.get)
        } else {
            Syslog.e("Could not find a config file. Exiting.")
            System.exit(1)
        }

        Syslog.i("started")
        System.gc()
    }

    def configure(file : File) {
        Syslog.i("Reading config " + file.getCanonicalFile())
        val e = new ScriptEngineManager().getEngineByName("scala")
        val settings = e.asInstanceOf[scala.tools.nsc.interpreter.IMain].settings
        settings.usejavacp.value = true
        val code = "import crossdns.config._\n" + scala.io.Source.fromFile(file).mkString
        e.eval(code)
    }

}
