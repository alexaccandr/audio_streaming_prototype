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

#include <jni.h>
#include "../../oboe/src/common/OboeDebug.h"
#include "PlaybackEngine.h"
#include "StreamingEngine.h"

static PlaybackEngine *playbackEngine = nullptr;
static StreamingEngine *streamingEngine = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1createPlaybackEngine(JNIEnv *env,
                                                                        jobject thiz,
                                                                        jint sampleRate) {
    if (playbackEngine == nullptr) {
        playbackEngine = new PlaybackEngine(sampleRate);
    }

    return static_cast<jboolean>(playbackEngine != nullptr);
}

JNIEXPORT void JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1deletePlaybackEngine(JNIEnv *env, jobject) {
    delete playbackEngine;
    playbackEngine = nullptr;
}

JNIEXPORT jboolean JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1startListening(JNIEnv *env, jobject thiz) {
    if (playbackEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return static_cast<jboolean>(false);
    }

    return static_cast<jboolean>(playbackEngine->startPlaying());
}

JNIEXPORT void JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1stopListening(JNIEnv *env, jobject thiz) {
    if (playbackEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return;
    }

    playbackEngine->stopPlaying();
}

JNIEXPORT void JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1pushData(JNIEnv *env,
                                                            jobject thiz,
                                                            jshortArray buffer_,
                                                            jint size) {
    if (playbackEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return;
    }

    jshort *buffer = env->GetShortArrayElements(buffer_, nullptr);
    playbackEngine->pushData(buffer, size);
    env->ReleaseShortArrayElements(buffer_, buffer, 0);
}

int64_t timeout = 2000000;
JNIEXPORT jint JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1writeData(JNIEnv *env,
                                                                          jobject thiz,
                                                                          jshortArray buffer_,
                                                                          jint size) {
    if (playbackEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return -1;
    }

//    auto * arr = new jshort[size];
    jshort* buffer = env->GetShortArrayElements(buffer_, nullptr);

    int32_t writeCount = playbackEngine->writeData(buffer, size,timeout);
//    LOGI("Actual read bytes from buffer: %d", readCount);
    env->ReleaseShortArrayElements(buffer_, buffer, 0);
    return static_cast<jint>(writeCount);
}

JNIEXPORT jboolean JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1createStreamingEngine(JNIEnv *env,
                                                                        jobject thiz,
                                                                        jint sampleRate,
                                                                        jint size) {
    if (streamingEngine == nullptr) {
        streamingEngine = new StreamingEngine(sampleRate, size);
    }

    return static_cast<jboolean>(streamingEngine != nullptr);
}

JNIEXPORT void JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1deleteStreamingEngine(JNIEnv *env, jobject) {
    delete streamingEngine;
    streamingEngine = nullptr;
}

JNIEXPORT jboolean JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1startStreaming(JNIEnv *env, jobject thiz) {
    if (streamingEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return static_cast<jboolean>(false);
    }

    return static_cast<jboolean>(streamingEngine->startStreaming());
}

JNIEXPORT void JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1setPlaybackOn(JNIEnv *env,
                                                                jobject thiz,
                                                                jboolean isOn) {
    if (streamingEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return;
    }

    streamingEngine->setPlaybackOn(isOn);
}

JNIEXPORT void JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1setMicrophoneOn(JNIEnv *env,
                                                              jobject thiz,
                                                              jboolean isOn) {
    if (streamingEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return;
    }

    streamingEngine->setMicrophoneOn(isOn);
}

JNIEXPORT jint JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1getRecordingData(JNIEnv *env,
                                                                jobject thiz,
                                                                jshortArray buffer_,
                                                                jint size,
                                                                jlong timeoutMs) {
    if (streamingEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return -1;
    }

    auto * arr = new jshort[size];
    int32_t readCount = streamingEngine->readData(arr, size, timeoutMs);
//    LOGI("Actual read bytes from buffer: %d", readCount);
    env->SetShortArrayRegion(buffer_, 0, size, arr);
    return static_cast<jint>(readCount);
}

JNIEXPORT void JNICALL
Java_com_example_androidtestoboetransmitter_AudioEngine_native_1stopStreaming(JNIEnv *env, jobject thiz) {
    if (streamingEngine == nullptr) {
        LOGE("Engine is null, you need to create a new one");
        return;
    }

    streamingEngine->stopStreaming();
}

}