package com.example.androidtestoboetransmitter

import android.content.Context

open class StreamLiveWorker(context: Context, sampleRate: Int, framesPerBufferInt: Int = 0) :
    StreamBaseWorker(context, sampleRate, framesPerBufferInt) {

    var started: Long = 0
    lateinit var name: String

    open fun initEngine() {}

    override fun onStreamStart() {
    }

    override fun onStreamError() {
    }


    override fun disconnect() {
        // Stop recording if needed
        super.disconnect()
    }
}