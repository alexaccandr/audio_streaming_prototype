package com.voxtours.vow.managers.streaming

import android.content.Context
import android.util.Log
import com.example.androidtestoboetransmitter.AudioEngine
import com.example.androidtestoboetransmitter.JavaUtils
import com.example.androidtestoboetransmitter.StreamLiveWorker
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket


class StreamLiveListenerWorker(context: Context, val sampleRate: Int, framesPerBufferInt: Int = 0) :
    StreamLiveWorker(context, sampleRate, framesPerBufferInt) {

    private var isListening = true

    override fun initEngine() {
        setupStreaming()
        readThread = ReadThread().also { it.start() }
        pushThread = PushThread().also { it.start() }
    }

    var readThread: Thread? = null
    var pushThread: Thread? = null
    override fun createThreads() {
    }

    inner class ReadThread : Thread() {

        var setupOnce = false
        override fun run() {
            Thread.currentThread().priority = MAX_PRIORITY
            acquireLock()
            if (AudioEngine.createPlaybackEngine(sampleRate)) {
                if (AudioEngine.startListening()) {
                } else {
                    errorMsg = "Unable to start PlaybackEngine."
                    Log.e(TAG, errorMsg)
                    onStreamError()
                }
            } else {
                errorMsg = "Unable to create PlaybackEngine."
                Log.e(TAG, errorMsg)
                onStreamError()
            }
            try {
                socket = MulticastSocket(11111)
                socket.joinGroup(InetAddress.getByName(MULTICAST_GROUP))
                socket.soTimeout = 5000
                val buffer = ByteArray(65535)
                val packet = DatagramPacket(buffer, buffer.size)
                while (!socket.isClosed && !isInterrupted) {
                    val time = System.currentTimeMillis()
                    socket.receive(packet)
                    val receivedTime = System.currentTimeMillis()
                    val data = packet.data
                    val len = packet.length
                    val offset = packet.offset

                    /* Check if job is still active, since cancellation may have occurred
                     * during packet receiving
                     */
                    if (!isInterrupted) {
                        var bytesLeft = len
                        var iteration = 0
                        while (bytesLeft > 0) {
                            iteration++
                            val dataToPlay = ByteArray(bytesLeft)
                            System.arraycopy(data, offset + (len - bytesLeft), dataToPlay, 0, bytesLeft)
                            val shortArray = JavaUtils.byteArrayToShortArray(dataToPlay)
                            val playTime = System.currentTimeMillis()
                            val writtenBytes = AudioEngine.writeShortArray(shortArray, shortArray.size) * 2
                            bytesLeft -= writtenBytes
                            val playTimeFinished = System.currentTimeMillis()
                            Log.e(
                                "Play time",
                                "Iter: ${iteration}, UDP time: (${receivedTime - time}) len: ${len}, playTime: (${playTimeFinished - playTime}, writtenBytes: ${writtenBytes})"
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "Stream lost"
                Log.i(TAG, errorMsg)
                onStreamError()
            } finally {
                Log.i(TAG, "Multicast finished")
            }

            releaseMulticastLock()
        }
    }

    inner class PushThread : Thread() {

        override fun run() {
            Thread.currentThread().priority = MAX_PRIORITY

            while (isAlive) {  // while job is not cancelled
                try {

//                    var data = ByteArray(buf.size)
//                    // TODO ?!
//
//                    val cnt = inStream!!.read(data, 0, data.size)
//                    if (cnt < data.size) {
//                        Log.e("WRITEN OOOPS!!!", "cnt < data.size)")
//                        val dataCopy = ByteArray(cnt)
//                        System.arraycopy(data, 0, dataCopy, 0, cnt)
//                        data = dataCopy
//                    }
//
//                    if (writenBytes != shortArray.size) {
//                        Log.e("WRONG", "${writenBytes} != ${shortArray.size}")
//                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        }
    }


    override fun disconnect() {
        super.disconnect()

//        // Stop AudioEngine stream and clean resources
        AudioEngine.stopListening()
        AudioEngine.deletePlaybackEngine()
        readThread?.interrupt()
        pushThread?.interrupt()
    }
}