package com.example.androidtestoboetransmitter

/**
 * Created by Gaetano Guida on 04/06/2019.
 */
object AudioEngine {

    init {
        System.loadLibrary("AudioEngine")
    }

    //region LISTENER
    /**
     *  Creates a new PlaybackEngine object. This will create an AudioStream object and setup all
     *  the features needed.
     *  You must call this before playing audio streaming.
     *
     *  @param sampleRate The sample rate of the streaming, in Hz
     *  @return true if PlaybackEngine was successfully created, false otherwise
     */
    fun createPlaybackEngine(sampleRate: Int): Boolean {
        return native_createPlaybackEngine(sampleRate)
    }

    /**
     *  Starts the AudioStream asynchronously.
     *  You must call this before playing audio streaming, after creating a PlaybackEngine object.
     *
     *  @return true if the stream started successfully, false otherwise
     */
    fun startListening(): Boolean {
        return native_startListening()
    }

    /**
     *  Stops the AudioStream asynchronously.
     *  Call this when you have finished playing audio streaming.
     */
    fun stopListening() {
        native_stopListening()
    }

    /**
     *  Deletes the current PlaybackEngine object.
     *  Call this when you have finished playing audio streaming, right after stopping the
     *  AudioStream.
     */
    fun deletePlaybackEngine() {
        native_deletePlaybackEngine()
    }

    /**
     *  Pushes data from the array to the PlaybackEngine queue, which will be used to provide data
     *  to be played on the AudioStream.
     *  Call this when receiving streaming audio data from the network.
     *
     *  @param buffer The buffer providing audio data
     *  @param length The length of the buffer
     */
    fun pushListeningData(buffer: ShortArray, length: Int) {
        native_pushData(buffer, length)
    }

    fun writeShortArray(shortArray: ShortArray, length: Int): Int {
        return native_writeData(shortArray, length)
    }

    // Native methods
    private external fun native_createPlaybackEngine(sampleRate: Int): Boolean

    private external fun native_deletePlaybackEngine()
    private external fun native_startListening(): Boolean
    private external fun native_stopListening()
    private external fun native_pushData(buffer: ShortArray, size: Int)
    private external fun native_writeData(shortArray: ShortArray, size: Int): Int
    //endregion

    //region PRESENTER
    /**
     *  Creates a new StreamingEngine object. This will create two AudioStream objects (one for
     *  recording and one for playing) and setup all the features needed.
     *  You must call this before starting a streaming session.
     *
     *  @param sampleRate The sample rate of the streaming, in Hz
     *  @param size The buffer size
     *  @return true if StreamingEngine was successfully created, false otherwise
     */
    fun createStreamingEngine(sampleRate: Int, size: Int): Boolean {
        return native_createStreamingEngine(sampleRate, size)
    }

    /**
     *  Starts the AudioStreams asynchronously.
     *  You must call this before recording audio streaming, after creating a StreamingEngine object.
     *
     *  @return true if the streams started successfully, false otherwise
     */
    fun startStreaming(): Boolean {
        return native_startStreaming()
    }

    /**
     *  Turns on/off the playback feature during streaming.
     *  Call this when the streaming is in progress.
     */
    fun setPlaybackOn(isOn: Boolean) {
        native_setPlaybackOn(isOn)
    }

    /**
     *  Turns on/off the audio input source during streaming.
     *  Call this when the streaming is in progress.
     */
    fun setMicrophoneOn(isOn: Boolean) {
        native_setMicrophoneOn(isOn)
    }

    /**
     *  Gets data from the StreamingEngine queue, which will be pushed to the network.
     *  Call this periodically when the streaming is in progress.
     *
     *  @param buffer The buffer to store recorded audio data
     *  @param size The size of the streaming buffer
     */
    fun getRecordingData(buffer: ShortArray, size: Int): Int {
        return native_getRecordingData(buffer, size)
    }

    /**
     *  Stops the AudioStreams asynchronously.
     *  Call this when you have finished recording audio streaming.
     */
    fun stopStreaming() {
        native_stopStreaming()
    }

    /**
     *  Deletes the current StreamingEngine object.
     *  Call this when you have finished recording audio streaming, right after stopping the
     *  AudioStreams.
     */
    fun deleteStreamingEngine() {
        native_deleteStreamingEngine()
    }

    // Native methods
    private external fun native_createStreamingEngine(sampleRate: Int, size: Int): Boolean

    private external fun native_deleteStreamingEngine()
    private external fun native_startStreaming(): Boolean
    private external fun native_setPlaybackOn(isOn: Boolean)
    private external fun native_setMicrophoneOn(isOn: Boolean)
    private external fun native_getRecordingData(buffer: ShortArray, size: Int): Int
    private external fun native_stopStreaming()
    //endregion
}
