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

        if (new File("./crossdns.conf").exists()) {
            configure(new File("./crossdns.conf"))
        } else {
            if (new File("/etc/crossdns.conf").exists()) {
                configure(new File("/etc/crossdns.conf"))
            } else {
                Syslog.e("Could not find a config file. Exiting.")
                System.exit(1)
            }
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
