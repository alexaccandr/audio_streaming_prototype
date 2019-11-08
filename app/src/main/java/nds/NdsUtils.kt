package nds

import android.net.nsd.NsdServiceInfo

object NdsUtils {

    fun registerService(port: Int) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName = "NsdChat"
            serviceType = "_nsdchat._udp"
            setPort(port)
        }
    }
}