package com.example.androidtestoboetransmitter

import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voxtours.vow.managers.streaming.StreamLiveListenerWorker
import com.voxtours.vow.managers.streaming.StreamLivePresenterWorker
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var createBroadcastThread: CreateBroadcastThread? = null
    var createListenerThread: CreateListenerThread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        startDiscovery.setOnClickListener {
            createBroadcastThread = CreateBroadcastThread(this).also { it.start() }
        }
        startListener.setOnClickListener {
            createListenerThread = CreateListenerThread(this).also { it.start() }
        }
    }

    class CreateBroadcastThread(context: Context) : Thread() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val streamBaseWorker = StreamLivePresenterWorker(
            context,
//            Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)),
            16000,
            Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER))
        )

        override fun run() {
            streamBaseWorker.initEngine()
        }
    }
    class CreateListenerThread(context: Context) : Thread() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val streamBaseWorker = StreamLiveListenerWorker(
            context,
//            Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)),
            16000,
            192
        )

        override fun run() {
            streamBaseWorker.initEngine()
        }
    }
}