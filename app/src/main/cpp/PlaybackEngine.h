//
// Created by Gaetano Guida on 12/02/2019.
//

#include <oboe/AudioStreamCallback.h>
#include <oboe/AudioStreamBuilder.h>
#include "LockFreeQueue.h"

#ifndef VOW_PLAYBACKENGINE_H
#define VOW_PLAYBACKENGINE_H

#endif

class PlaybackEngine {

    public:
        explicit PlaybackEngine(int32_t sampleRate);
        bool startPlaying();
        void stopPlaying();
        void pushData(short *buf, int size);
    int32_t writeData(short *buf, int size);

    private:
        oboe::AudioStream *mPlaybackStream = nullptr;
        int32_t mSampleRate;
        static int const MAX_QUEUE_ITEMS = 4096 * 2;
        LockFreeQueue<int16_t, MAX_QUEUE_ITEMS> mAudioDataQueue;

        bool openStream();
        bool startStream();
        void stopStream();
        void closeStream();
};