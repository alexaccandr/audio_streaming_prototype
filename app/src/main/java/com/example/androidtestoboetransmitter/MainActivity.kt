package com.example.androidtestoboetransmitter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
            createBroadcastThread = CreateBroadcastThread(this)
            createBroadcastThread?.start()
        }
        startListener.setOnClickListener {
            createListenerThread = CreateListenerThread(this)
            createListenerThread?.start()
        }

        if (!isRecordPermissionGranted()) run { requestPermissions() }
    }

    val APP_PERMISSION_REQUEST = 1234
    private fun isRecordPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            APP_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (APP_PERMISSION_REQUEST != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(applicationContext, "AAAA", Toast.LENGTH_SHORT).show()
        }
    }


    class CreateBroadcastThread(context: Context) : Thread() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val streamBaseWorker = StreamLivePresenterWorker(
            context,
//            Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)),
            16000,
            192
        )

        override fun run() {
            priority = MAX_PRIORITY
            streamBaseWorker.initEngine()
        }
    }

    class CreateListenerThread(context: Context) : Thread() {

        val streamBaseWorker = StreamLiveListenerWorker(
            context,
//            Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)),
            16000,
            192
        )

        override fun run() {
            priority = MAX_PRIORITY
            streamBaseWorker.initEngine()
        }
    }
}