package com.example.androidtestoboetransmitter

import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.util.Log
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

open class StreamBaseWorker(context: Context, sampleRate: Int, var framesPerBufferInt: Int = 0) {

    private var audioManager: AudioManager
    private var wifiManager: WifiManager
    protected lateinit var buf: ShortArray
    protected lateinit var socket: MulticastSocket
    private var group: InetAddress? = null
    protected var inStream: PipedInputStream? = null
    protected var outStream: PipedOutputStream? = null
    protected lateinit var errorMsg: String
    private var multiCastLock: WifiManager.MulticastLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        private const val SAMPLE_INTERVAL = 20L // milliseconds
        private const val SAMPLE_SIZE = 2 // bytes per sample
        const val BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE
        const val MULTICAST_GROUP = "224.0.0.251"
        const val TAG = "StreamLiveWorker"
    }

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (framesPerBufferInt == 0)
            framesPerBufferInt = 256   // default

        // Adjust buffer size to be multiple of device's optimal one
        var factor = BUF_SIZE / framesPerBufferInt

        if (factor == 0L)
            factor = 1

        buf = ShortArray((framesPerBufferInt * factor).toInt())
    }

    protected fun initSocket(): Int {
        socket = MulticastSocket(11111)
        group = InetAddress.getByName(MULTICAST_GROUP)
        socket.joinGroup(group)
        return socket.localPort
    }

    open fun setupStreaming() {
        try {
            setupPipedStreams()
            createThreads()
        } catch (e: IOException) {
            e.printStackTrace()
            errorMsg = e.localizedMessage
            onStreamError()
        }
    }

    open fun createThreads() {
    }

    open fun setupPipedStreams() {
        inStream = PipedInputStream(buf.size)
        outStream = PipedOutputStream(inStream)
    }

    fun acquireLock() {
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LockTag")
        wifiLock!!.acquire()

        if (wifiLock!!.isHeld) {
            Log.i(TAG, "Wifi wifiLock acquired")
        }

        multiCastLock = wifiManager.createMulticastLock("com.vow.VOWListener")
        multiCastLock!!.setReferenceCounted(true)
        multiCastLock!!.acquire()

        if (multiCastLock!!.isHeld) {
            Log.i(TAG, "Multicast wifiLock acquired")
        }
    }

    fun releaseMulticastLock() {
        if (multiCastLock != null) {
            multiCastLock = if (multiCastLock!!.isHeld) {
                multiCastLock!!.release()
                null
            } else {
                null
            }
        }
    }

    protected fun sendPacket(data: ByteArray) {
        val packet = DatagramPacket(data, data.size, group, 11111)
        socket.send(packet)
    }

    open fun getSampleInterval(): Long {
        return SAMPLE_INTERVAL
    }

    open fun disconnect() {
        synchronized(this) {
            // TODO Cancel threads

            // Release Multicast Lock
            releaseMulticastLock()

            try {
                // Clean MultiCast UDP socket
                val address = InetAddress.getByName(MULTICAST_GROUP)
                if (address != null)
                    socket.leaveGroup(address)

                // Close streams
                inStream?.close()
                inStream = null
                outStream?.close()
                outStream = null

            } catch (e: IOException) {
                e.printStackTrace()
            }

            // Close socket and release WiFi lock
            socket.close()
            wifiLock?.release()
        }
    }

    open fun onStreamStart() {}

    open fun onStreamError() {}
}