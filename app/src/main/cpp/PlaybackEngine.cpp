//
// Created by Gaetano Guida on 12/02/2019.
//

#include "PlaybackEngine.h"
#include "../../oboe/src/common/OboeDebug.h"
#include <oboe/Oboe.h>

PlaybackEngine::PlaybackEngine(int32_t sampleRate) {
    mSampleRate = sampleRate;
}

bool PlaybackEngine::startPlaying() {
    if (openStream()) {
        return startStream();
    } else {
        return false;
    }
}

bool PlaybackEngine::openStream() {
    oboe::AudioStreamBuilder builder;
    builder.setCallback(this);
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::I16);
    builder.setChannelCount(oboe::ChannelCount::Mono);
    builder.setContentType(oboe::ContentType::Speech);
    builder.setSampleRate(mSampleRate);

    oboe::Result result = builder.openStream(&mPlaybackStream);

    if (result != oboe::Result::OK) {
        LOGE("Error opening stream: %s", oboe::convertToText(result));
        return false;
    }

    return true;
}

bool PlaybackEngine::startStream() {
    if (mPlaybackStream) {
        mPlaybackStream->setBufferSizeInFrames(mPlaybackStream->getFramesPerBurst() * 2);
        oboe::Result result = mPlaybackStream->requestStart();

        if (result != oboe::Result::OK) {
            LOGE("Error opening stream: %s", oboe::convertToText(result));
            return false;
        }

        return true;
    } else {
        LOGE("Error: cannot open null stream");
        return false;
    }
}

void PlaybackEngine::pushData(short *buf, int size) {
    for (int i = 0; i < size; ++i) {
        mAudioDataQueue.push(buf[i]);
    }
}

void PlaybackEngine::stopPlaying() {
    stopStream();
    closeStream();
}

void PlaybackEngine::stopStream() {
    if (mPlaybackStream) {
        oboe::Result result = mPlaybackStream->stop(0L);

        if (result != oboe::Result::OK) {
            LOGE("Error stopping stream. %s", oboe::convertToText(result));
        }
    }
}

void PlaybackEngine::closeStream() {
    if (mPlaybackStream) {
        oboe::Result result = mPlaybackStream->close();

        if (result != oboe::Result::OK) {
            LOGE("Error closing stream. %s", oboe::convertToText(result));
        }
    }
}

oboe::DataCallbackResult PlaybackEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {

    // We requested AudioFormat::I16 so we assume we got it. For production code always check what format
    // the stream has and cast to the appropriate type.

    auto *outputData = static_cast<int16_t *>(audioData);
    int16_t nextFrame;
    int32_t framesRead = 0;

    // Read data from LockFreeQueue and write it to outputData
    for (int i = 0; i < numFrames; ++i) {
        bool popped = mAudioDataQueue.pop(nextFrame);

        if (popped) {
            outputData[i] = nextFrame;
            framesRead++;
        } else {
            // Handle the case of no data
            break;
        }
    }

    int32_t framesToPad = numFrames - framesRead;

    LOGE("Queue size: %d, Read frames: %d, frames to pad: %d", mAudioDataQueue.size(), framesRead,framesToPad);

    if (framesToPad > 0) {
        int32_t samplesRead = framesRead * audioStream->getChannelCount();
        int16_t *padPos = outputData + samplesRead;
        auto size = static_cast<size_t>(framesToPad * audioStream->getBytesPerFrame());

        // pad the buffer with zeros
        memset(padPos, 0, size);
    }

    return oboe::DataCallbackResult::Continue;
}