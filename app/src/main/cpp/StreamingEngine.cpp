/**
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "StreamingEngine.h"
#include <cassert>
#include "../../oboe/src/common/OboeDebug.h"
#include <climits>

StreamingEngine::StreamingEngine(int32_t sampleRate, int32_t bufferSize) {
    assert(mOutputChannelCount == mInputChannelCount);
    mSampleRate = sampleRate;
    mBufferSize = bufferSize;
}

StreamingEngine::~StreamingEngine() {
    //stopStream(mPlayStream);
    stopStream(mRecordingStream);

    //closeStream(mPlayStream);

    closeStream(mRecordingStream);
}

void StreamingEngine::setRecordingDeviceId(int32_t deviceId) {
    mRecordingDeviceId = deviceId;
}

void StreamingEngine::setPlaybackDeviceId(int32_t deviceId) {
    mPlaybackDeviceId = deviceId;
}

void StreamingEngine::setPlaybackOn(bool isOn) {
    isPlaybackOn = isOn;
}

void StreamingEngine::setMicrophoneOn(bool isOn) {
    isMicrophoneOn = isOn;

    oboe::Result result;
    char* action;

    if (isMicrophoneOn) {
        action = const_cast<char *>("resuming");
        result = mRecordingStream->requestStart();
    } else {
        action = const_cast<char *>("pausing");
        result = mRecordingStream->requestPause();
    }

    if (result != oboe::Result::OK) {
        LOGE("Error %s stream. %s", action, oboe::convertToText(result));
    }
}

void StreamingEngine::readData(short *buf, int size) {
    oboe::ResultWithValue<int32_t> status = mRecordingStream->read(buf, size, 0);

    if (!status) {
        LOGE("input stream read error: %s", oboe::convertToText(status.error()));
    }
}

bool StreamingEngine::startStreaming() {
    return openAllStreams();
}

void StreamingEngine::stopStreaming() {
    closeAllStreams();
}

bool StreamingEngine::openAllStreams() {
    // Note: The order of stream creation is important. We create the playback
    // stream first, then use properties from the playback stream
    // (e.g. sample rate) to create the recording stream. By matching the
    // properties we should get the lowest latency path

    //if (!openPlaybackStream())
    //  return false;

    if (!openRecordingStream())
        return false;

    // Now start the recording stream first so that we can read from it during
    // the playback stream's dataCallback
    if (mRecordingStream /*&& mPlayStream*/) {
        return startStream(mRecordingStream);// && startStream(mPlayStream);
    } else {
        LOGE("Failed to create recording and/or playback stream");
        closeAllStreams();
        return false;
    }
}

/**
 * Stops and closes the playback and recording streams.
 */
void StreamingEngine::closeAllStreams() {
    /**
     * Note: The order of events is important here.
     * The playback stream must be closed before the recording stream. If the
     * recording stream were to be closed first the playback stream's
     * callback may attempt to read from the recording stream
     * which would cause the app to crash since the recording stream would be
     * null.
     */

    /*if (mPlayStream != nullptr) {
        closeStream(mPlayStream);  // Calling close will also stop the stream
        mPlayStream = nullptr;
    }*/

    if (mRecordingStream != nullptr) {
        closeStream(mRecordingStream);
        mRecordingStream = nullptr;
    }
}

/**
 * Creates an audio stream for recording. The audio device used will depend on
 * mRecordingDeviceId.
 * If the value is set to oboe::Unspecified then the default recording device
 * will be used.
 */
bool StreamingEngine::openRecordingStream() {
    // To create a stream we use a stream builder. This allows us to specify all
    // the parameters for the stream prior to opening it
    oboe::AudioStreamBuilder builder;

    setupRecordingStreamParameters(&builder);

    // Now that the parameters are set up we can open the stream
    oboe::Result result = builder.openStream(&mRecordingStream);
    if (result == oboe::Result::OK && mRecordingStream) {
        assert(mRecordingStream->getChannelCount() == mInputChannelCount);
        assert(mRecordingStream->getSampleRate() == mSampleRate);
        assert(mRecordingStream->getFormat() == oboe::AudioFormat::I16);

        warnIfNotLowLatency(mRecordingStream);
        return true;
    } else {
        LOGE("Failed to create recording stream. Error: %s",
             oboe::convertToText(result));
        return false;
    }
}

/**
 * Creates an audio stream for playback. The audio device used will depend on
 * mPlaybackDeviceId.
 * If the value is set to oboe::Unspecified then the default playback device
 * will be used.
 */
bool StreamingEngine::openPlaybackStream() {
    oboe::AudioStreamBuilder builder;

    setupPlaybackStreamParameters(&builder);
    oboe::Result result = builder.openStream(&mPlayStream);
    if (result == oboe::Result::OK && mPlayStream) {

        assert(mPlayStream->getFormat() == oboe::AudioFormat::I16);
        assert(mOutputChannelCount == mPlayStream->getChannelCount());

        warnIfNotLowLatency(mPlayStream);
        return true;

    } else {
        LOGE("Failed to create playback stream. Error: %s",
             oboe::convertToText(result));
        return false;
    }
}

/**
 * Sets the stream parameters which are specific to recording,
 * including the sample rate which is determined from the
 * playback stream.
 *
 * @param builder The recording stream builder
 */
oboe::AudioStreamBuilder *StreamingEngine::setupRecordingStreamParameters(
    oboe::AudioStreamBuilder *builder) {

    // We set a callback to get audio data
    builder->setCallback(nullptr)
        ->setBufferCapacityInFrames(mBufferSize)
        ->setContentType(oboe::ContentType::Speech)
//        ->setDeviceId(mRecordingDeviceId)
        ->setDirection(oboe::Direction::Input)
        ->setSampleRate(mSampleRate)
        ->setChannelCount(mInputChannelCount);
    return setupCommonStreamParameters(builder);
}

/**
 * Sets the stream parameters which are specific to playback, including device
 * id and the dataCallback function, which must be set for low latency
 * playback.
 * @param builder The playback stream builder
 */
oboe::AudioStreamBuilder *StreamingEngine::setupPlaybackStreamParameters(
    oboe::AudioStreamBuilder *builder) {
    builder//->setCallback(this)
//        ->setDeviceId(mPlaybackDeviceId)
        ->setDirection(oboe::Direction::Output)
        ->setSampleRate(mSampleRate)
        ->setChannelCount(mOutputChannelCount);

    return setupCommonStreamParameters(builder);
}

/**
 * Set the stream parameters which are common to both recording and playback
 * streams.
 * @param builder The playback or recording stream builder
 */
oboe::AudioStreamBuilder *StreamingEngine::setupCommonStreamParameters(
    oboe::AudioStreamBuilder *builder) {
    // We request EXCLUSIVE mode since this will give us the lowest possible
    // latency.
    // If EXCLUSIVE mode isn't available the builder will fall back to SHARED
    // mode.
    builder->setFormat(mFormat)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency);
    return builder;
}

bool StreamingEngine::startStream(oboe::AudioStream *stream) {
    assert(stream);
    oboe::Result result = stream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Error starting stream. %s", oboe::convertToText(result));
        return false;
    }

    return true;
}

void StreamingEngine::stopStream(oboe::AudioStream *stream) {
    if (stream) {
        oboe::Result result = stream->stop(0L);
        if (result != oboe::Result::OK) {
            LOGE("Error stopping stream. %s", oboe::convertToText(result));
        }
    }
}

/**
 * Close the stream. AudioStream::close() is a blocking call so
 * the application does not need to add synchronization between
 * onAudioReady() function and the thread calling close().
 * [the closing thread is the UI thread in this sample].
 * @param stream the stream to close
 */
void StreamingEngine::closeStream(oboe::AudioStream *stream) {
    if (stream) {
        oboe::Result result = stream->close();
        if (result != oboe::Result::OK) {
            LOGE("Error closing stream. %s", oboe::convertToText(result));
        }
    }
}

/**
 * Restart the streams. During the restart operation subsequent calls to this
 * method will output a warning.
 */
void StreamingEngine::restartStreams() {
    LOGI("Restarting streams");

    if (mRestartingLock.try_lock()) {
        closeAllStreams();
        openAllStreams();
        mRestartingLock.unlock();
    } else {
        LOGW(
            "Restart stream operation already in progress - ignoring this "
            "request");
        // We were unable to obtain the restarting lock which means the restart
        // operation is currently
        // active. This is probably because we received successive "stream
        // disconnected" events.
        // Internal issue b/63087953
    }
}

/**
 * Warn in logcat if non-low latency stream is created
 * @param stream: newly created stream
 *
 */
void StreamingEngine::warnIfNotLowLatency(oboe::AudioStream *stream) {
    if (stream->getPerformanceMode() != oboe::PerformanceMode::LowLatency) {
        LOGW(
            "Stream is NOT low latency."
            "Check your requested format, sample rate and channel count");
    }
}

/**
 * Handles playback stream's audio request. In this sample, we simply block-read
 * from the record stream for the required samples.
 *
 * @param oboeStream: the playback stream that requesting additional samples
 * @param audioData:  the buffer to load audio samples for playback stream
 * @param numFrames:  number of frames to load to audioData buffer
 * @return: DataCallbackResult::Continue.
 */
oboe::DataCallbackResult StreamingEngine::onAudioReady(
    oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {

    /*assert(oboeStream == mRecordingStream);
    auto *inputData = static_cast<int16_t *>(audioData);

    for (int i = 0; i < numFrames; ++i) {
        mAudioDataQueue.push(inputData[i]);
    }

    // if (isPlaybackOn)
    oboe::ResultWithValue<int32_t> status = mPlayStream->write(inputData, numFrames, 0);
    if (!status) {
        LOGE("playback stream write error: %s", oboe::convertToText(status.error()));
    }*/

    return oboe::DataCallbackResult::Continue;
}

/**
 * Oboe notifies the application for "about to close the stream".
 *
 * @param oboeStream: the stream to close
 * @param error: oboe's reason for closing the stream
 */
void StreamingEngine::onErrorBeforeClose(oboe::AudioStream *oboeStream,
                                          oboe::Result error) {
    LOGE("%s stream Error before close: %s",
         oboe::convertToText(oboeStream->getDirection()),
         oboe::convertToText(error));
}

/**
 * Oboe notifies application that "the stream is closed"
 *
 * @param oboeStream
 * @param error
 */
void StreamingEngine::onErrorAfterClose(oboe::AudioStream *oboeStream,
                                         oboe::Result error) {
    LOGE("%s stream Error after close: %s",
         oboe::convertToText(oboeStream->getDirection()),
         oboe::convertToText(error));
}