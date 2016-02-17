#include <stdio.h>
#include <sstream>

#include <soundtouch/SoundTouch.h>
#include <soundtouch/BPMDetect.h>

#include "WavFile.h"

namespace KitchenSync
{
    using namespace soundtouch;

    // Processing chunk size (size chosen to be divisible by 2, 4, 6, 8, 10, 12, 14, 16 channels ...)
    #define BUFF_SIZE           6720

    static SoundTouch soundTouch;

    typedef struct {
        char  *inFileName;
        char  *outFileName;
        float tempoDelta;
        float pitchDelta;
        float rateDelta;
        int   quick;
        int   noAntiAlias;
        float goalBPM;
        bool  detectBPM;
        bool  speech;
    } RunParameters;

    const char *getVersion();
    float getBpm(const char *filename);
    void setBpm(const char *inFilename, const char *outFilename, float fromBpm, float toBpm);
}