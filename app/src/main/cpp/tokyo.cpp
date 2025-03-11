#include <jni.h>
#include <string>
#include <vector>

// Obfuscated developer credits (encoded with position-based XOR)
static const unsigned char obfuscated_dev[] = {
    // "lyssadev & chifft" encoded
    0x6C ^ 0xFF, 0x79 ^ 0xFF, 0x73 ^ 0xFF, 0x73 ^ 0xFF, 0x61 ^ 0xFF,
    0x64 ^ 0xFF, 0x65 ^ 0xFF, 0x76 ^ 0xFF, 0x20 ^ 0xFF, 0x26 ^ 0xFF,
    0x20 ^ 0xFF, 0x63 ^ 0xFF, 0x68 ^ 0xFF, 0x69 ^ 0xFF, 0x66 ^ 0xFF,
    0x66 ^ 0xFF, 0x74 ^ 0xFF
};

// Deobfuscation key (constant XOR key)
static const unsigned char dev_key[] = {
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
};

static std::string deobfuscate_dev() {
    std::vector<char> result;
    for(size_t i = 0; i < sizeof(obfuscated_dev); i++) {
        // Simple XOR deobfuscation
        char c = obfuscated_dev[i] ^ dev_key[i];
        result.push_back(c);
    }
    return std::string(result.begin(), result.end());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_noxpeteam_txpacker_NativeTextProvider_getDeveloperCredits(
        JNIEnv* env,
        jclass /* this */) {
    return env->NewStringUTF(deobfuscate_dev().c_str());
} 