package crossdns.util

import java.util.concurrent.locks.ReentrantLock

class Lock {

    private val lock = new ReentrantLock

    def locked(f : => Unit) : Unit = {
        try {
            lock.lock()
            f
        } finally {
            try { lock.unlock() } catch {case _ : Throwable => {}}
        }
    }

}
