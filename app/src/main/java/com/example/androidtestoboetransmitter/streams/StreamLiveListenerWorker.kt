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
import java.nio.ByteBuffer


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
                val byteBuffer = ByteBuffer.allocate(1024 * 16)
                while (!socket.isClosed && !isInterrupted) {
                    socket.receive(packet)

                    val data = packet.data
                    val len = packet.length
                    val offset = packet.offset

                    /* Check if job is still active, since cancellation may have occurred
                     * during packet receiving
                     */
//                    Log.e(TAG, "Read bytes: $len, offset: $offset")
                    if (!isInterrupted) {
//                        outStream!!.write(data, offset, len)
//                        val data2 = ByteArray(len)
//                        System.arraycopy(data, offset, data2, 0, len)
                        byteBuffer.put(data, offset, len)
                        if (!setupOnce) {
                            if (byteBuffer.position() < 1024 * 8) {
                                continue
                            } else {
                                setupOnce = true
                            }
                            continue
                        }
                        byteBuffer.flip()
                        val bufferToSend = ByteArray(byteBuffer.limit())
                        byteBuffer.get(bufferToSend)
                        outStream!!.write(bufferToSend)
                        outStream!!.flush()
                        byteBuffer.clear()
//                        if (len < 200)
//                            println("Data size $len")
                    }
//                    sleep(getSampleInterval())
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

                    var data = ByteArray(buf.size)
                    // TODO ?!

                    val cnt = inStream!!.read(data, 0, data.size)
                    if (cnt < data.size) {
                        Log.e("WRITEN OOOPS!!!", "cnt < data.size)")
                        val dataCopy = ByteArray(cnt)
                        System.arraycopy(data, 0, dataCopy, 0, cnt)
                        data = dataCopy
                    }

                    val shortArray = JavaUtils.byteArrayToShortArray(data)
                    val writenBytes = AudioEngine.writeShortArray(shortArray, shortArray.size)
                    if (writenBytes != shortArray.size) {
                        Log.e("WRONG", "${writenBytes} != ${shortArray.size}")
                    }

//                    val shortBuf = JavaUtils.byteArrayToShortArray(data)
//                    for (i in shortBuf.indices) {
//                        buf[i] = shortBuf[i]
//                    }

//                    val data = ByteArray(buf.size)
//                    val cnt = inStream!!.read(data, 0, buf.size)
//
//                    for (i in 0 until cnt) {
//                        buf[i] = G711.alaw2linear(data[i])
//                    }
//
//                    /* Check if job is still active, since cancellation may have occurred
//                     * during buffer computation
//                     */
//                    if (isAlive) {
//                        Log.e(TAG, "Play bytes: ${data.size}")
//                        // Write buffer data to AudioEngine LockFreeQueue
//                        AudioEngine.pushListeningData(buf, cnt)
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