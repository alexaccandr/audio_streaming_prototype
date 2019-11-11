package com.voxtours.vow.managers.streaming

import android.content.Context
import android.util.Log
import com.example.androidtestoboetransmitter.AudioEngine
import com.example.androidtestoboetransmitter.JavaUtils
import com.example.androidtestoboetransmitter.StreamLiveWorker
import java.io.IOException


open class StreamLivePresenterWorker(
    val context: Context,
    val sampleRate: Int,
    framesPerBufferInt: Int = 0
) :
    StreamLiveWorker(context, sampleRate, framesPerBufferInt) {

    private var isMicrophoneOn = true

    override fun initEngine() {
        if (AudioEngine.createStreamingEngine(
                sampleRate,
                buf.size
            )
        ) { // TODO Find the best buffer size
            if (AudioEngine.startStreaming()) {
                setupStreaming()
            } else {
                errorMsg = "Unable to start StreamingEngine."
                Log.e(TAG, errorMsg)
                onStreamError()
            }
        } else {
            errorMsg = "Unable to create StreamingEngine."
            Log.e(TAG, errorMsg)
            onStreamError()
        }
    }

    lateinit var pushThread: PushThread
    lateinit var readThread: ReadThread

    override fun createThreads() {
        readThread = ReadThread().also { it.start() }
        pushThread = PushThread().also { it.start() }
    }

    inner class ReadThread : Thread() {

        override fun run() {
//            Thread.currentThread().priority = MAX_PRIORITY
            acquireLock()
            initSocket()

            while (!isInterrupted) {  // while job is not cancelled
                try {
                    val startData = System.currentTimeMillis()
                    // Get audio data from recording stream
                    var readBytes = AudioEngine.getRecordingData(buf, buf.size)
                    if (readBytes == 0) {
                        readBytes = buf.indexOfLast { it != 0.toShort() }
                        if (readBytes == -1) {
                            readBytes = buf.size
                        }
                        Log.e(TAG, "Last Index: $readBytes")
                    } else {
                        Log.e(TAG, "Actual bytes: $readBytes")
                    }

//                    val len = buf.size
//                    val data = JavaUtils.shortToByte_Twiddle_Method(buf)
                    val len = readBytes
                    val shortArray = ShortArray(len)
                    for (i in 0 until len) {
                        shortArray[i] = buf[i]
                    }
                    val data = JavaUtils.shortToByte_Twiddle_Method(shortArray)
//                    for (i in 0 until len) {
//                        data[i] = G711.linear2alaw(buf[i])
//                    }

                    if (!isInterrupted) {
                        Log.e(TAG, "readPackets: ${data.size}")
                        // Push audio data to pipe
//                        outStream?.write(data, 0, len)
                        sendPacket(data)
                    }

                    val executeTime = System.currentTimeMillis() - startData
//                    sleep(getSampleInterval() - executeTime)
                } catch (e: IOException) {
                    e.printStackTrace()
                    errorMsg = "Unable to get recording data"
                    Log.i(TAG, errorMsg)
                    onStreamError()
                }
            }
        }
    }

    inner class PushThread : Thread() {
        override fun run() {
            Thread.currentThread().priority = MAX_PRIORITY
//            acquireLock()
//
//            try {
//                initSocket()
//
//                while (!isInterrupted) {  // while job is not cancelled
//                    val data = ByteArray(buf.size)
//                    // Every time we have new data in the pipe, send it
//                    inStream!!.read(data, 0, data.size)
//
//                    if (isMicrophoneOn) {
//                        sendPacket(data)
//                        Log.e(TAG, "sendPackets: ${data.size}")
//                    }
//                }
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                errorMsg = "Stream error"
//                Log.i(TAG, errorMsg)
//                onStreamError()
//            } finally {
//                Log.i(TAG, "Multicast finished")
//            }
        }
    }

    override fun disconnect() {
        super.disconnect()

        // Stop AudioEngine stream and clean resources
        AudioEngine.stopStreaming()
        AudioEngine.deleteStreamingEngine()
    }
}