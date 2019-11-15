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
//    builder.setCallback(this);
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

int32_t PlaybackEngine::writeData(short *buf, int size, int64_t timeout) {
    oboe::ResultWithValue<int32_t> status = mPlaybackStream->write(buf, size, timeout); // 20 ms timeout

    if (!status) {
        LOGE("input stream read error: %s", oboe::convertToText(status.error()));
        return -1;
    }
    return status.value();
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