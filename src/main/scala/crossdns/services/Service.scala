package crossdns.services

trait Service extends Runnable {
    val serviceName : String
    var thread : Thread = _
    var runService = true

    override def run() {
        while (runService) try {
            act
        } catch {
            case t : Throwable => t.printStackTrace()
        }
    }
    def act()

    def start() = {
        runService = true
        if (thread != null && thread.isAlive()) {
            try { thread.interrupt() } catch { case t : Throwable => t.printStackTrace() }
        }
        thread = new Thread(this, serviceName)
        thread.start()
    }

    def stop() = {
        runService = false
        if (thread.isAlive()) {
            try { thread.interrupt() } catch { case t : Throwable => t.printStackTrace() }
        }
    }

}
