/*
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

#ifndef OBOE_STREAMINGENGINE_H
#define OBOE_STREAMINGENGINE_H

#include <jni.h>
#include <oboe/Oboe.h>
#include <string>
#include <thread>
#include "LockFreeQueue.h"

class StreamingEngine : public oboe::AudioStreamCallback {
   public:
    StreamingEngine(int32_t sampleRate, int32_t bufferSize);
    ~StreamingEngine();
    void setRecordingDeviceId(int32_t deviceId);
    void setPlaybackDeviceId(int32_t deviceId);
    void setPlaybackOn(bool isOn);
    void setMicrophoneOn(bool isOn);
    int32_t readData(short *buf, int size);
    bool startStreaming();
    void stopStreaming();

    /*
     * oboe::AudioStreamCallback interface implementation
     */
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream,
                                          void *audioData, int32_t numFrames);
    void onErrorBeforeClose(oboe::AudioStream *oboeStream, oboe::Result error);
    void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error);

   private:
    bool isPlaybackOn = false;
    bool isMicrophoneOn = true;
    int32_t mRecordingDeviceId = oboe::kUnspecified;
    int32_t mPlaybackDeviceId = oboe::kUnspecified;
    oboe::AudioFormat mFormat = oboe::AudioFormat::I16;
    int32_t mSampleRate = oboe::kUnspecified;
    int32_t mBufferSize = oboe::kUnspecified;
    int32_t mInputChannelCount = oboe::ChannelCount::Mono;
    int32_t mOutputChannelCount = oboe::ChannelCount::Mono;
    oboe::AudioStream *mRecordingStream = nullptr;
    oboe::AudioStream *mPlayStream = nullptr;
    std::mutex mRestartingLock;

    bool openRecordingStream();
    bool openPlaybackStream();

    bool startStream(oboe::AudioStream *stream);
    void stopStream(oboe::AudioStream *stream);
    void closeStream(oboe::AudioStream *stream);

    bool openAllStreams();
    void closeAllStreams();
    void restartStreams();

    oboe::AudioStreamBuilder *setupCommonStreamParameters(
        oboe::AudioStreamBuilder *builder);
    oboe::AudioStreamBuilder *setupRecordingStreamParameters(
        oboe::AudioStreamBuilder *builder);
    oboe::AudioStreamBuilder *setupPlaybackStreamParameters(
        oboe::AudioStreamBuilder *builder);
    void warnIfNotLowLatency(oboe::AudioStream *stream);
};

#endif  // OBOE_LIVEEFFECTENGINE_H
