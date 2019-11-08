//
// Created by Gaetano Guida on 12/02/2019.
//

#include <oboe/AudioStreamCallback.h>
#include <oboe/AudioStreamBuilder.h>
#include "LockFreeQueue.h"

#ifndef VOW_PLAYBACKENGINE_H
#define VOW_PLAYBACKENGINE_H

#endif

class PlaybackEngine : oboe::AudioStreamCallback {

    public:
        explicit PlaybackEngine(int32_t sampleRate);
        bool startPlaying();
        void stopPlaying();
        void pushData(short *buf, int size);
        oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

    private:
        oboe::AudioStream *mPlaybackStream = nullptr;
        int32_t mSampleRate;
        static int const MAX_QUEUE_ITEMS = 2048;
        LockFreeQueue<int16_t, MAX_QUEUE_ITEMS> mAudioDataQueue;

        bool openStream();
        bool startStream();
        void stopStream();
        void closeStream();
};