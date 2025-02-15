#include "MP3Decoder.h"

MP3Decoder::MP3Decoder(const char *filename) {
    this->filename = filename;
    mp3 = fopen(filename, "r");
    hip = hip_decode_init();

    readHeaders();
}

MP3Decoder::~MP3Decoder() {
    fclose(mp3);
    hip_decode_exit(hip);
}

void MP3Decoder::readHeaders() {
    unsigned char headerBuf[1024];
    int len;

    do {
        len = fread(headerBuf, sizeof(unsigned char), 1024, mp3);

        short pcm_buffer_l[PCM_SIZE];
        short pcm_buffer_r[PCM_SIZE];

        hip_decode1_headers(hip, headerBuf, len, pcm_buffer_l, pcm_buffer_r, &mp3data);
    } while (mp3data.header_parsed == 0 && len > 0);

    if (mp3data.header_parsed == 0) {
        throw std::runtime_error(std::string("Unable to parse mp3 headers for ") + std::string(filename));
    }

    this->setNumSamples((uint) (mp3data.framesize * mp3data.totalframes));
    this->setSampleRate((uint) mp3data.samplerate);
    this->setNumChannels((uint) mp3data.stereo);
    this->setNumBits((uint) 16);
}

int MP3Decoder::eof() const {
    return buffer.empty() && feof(mp3);
}

int MP3Decoder::decodeChunk() {
    int read;
    int decoded;
    unsigned char mp3_buffer[MP3_SIZE];
    short pcm_buffer_l[PCM_SIZE];
    short pcm_buffer_r[PCM_SIZE];

    read = fread(mp3_buffer, sizeof(unsigned char), MP3_SIZE, mp3);

    decoded = hip_decode(hip, mp3_buffer, read, pcm_buffer_l, pcm_buffer_r);

    for (int i = 0; i < decoded; i++) {
        buffer.push_back((float) _swap16(pcm_buffer_l[i]) * this->getConv());
        buffer.push_back((float) _swap16(pcm_buffer_r[i]) * this->getConv());
    }

    return decoded;
}

int MP3Decoder::read(float *buf, int len) {
    if (buffer.empty()) {
        if (decodeChunk() == 0) {
            return 0;
        }
    }

    int i;

    for (i = 0; !buffer.empty() && i < len; i++) {
        buf[i] = buffer.at(0);
        buffer.erase(buffer.begin());
    }

    return i;
}

void MP3Decoder::rewind() {
    fseek(mp3, SEEK_SET, 0);
}
