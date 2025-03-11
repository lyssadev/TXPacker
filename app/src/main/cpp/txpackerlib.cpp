#include <jni.h>
#include <string>
#include <vector>

// Obfuscated team credit text (encoded with position-based XOR)
static const unsigned char obfuscated_team[] = {
    // "from the NoxPE Team" encoded
    0x66 ^ 0xFF, 0x72 ^ 0xFF, 0x6F ^ 0xFF, 0x6D ^ 0xFF, 0x20 ^ 0xFF,
    0x74 ^ 0xFF, 0x68 ^ 0xFF, 0x65 ^ 0xFF, 0x20 ^ 0xFF, 0x4E ^ 0xFF,
    0x6F ^ 0xFF, 0x78 ^ 0xFF, 0x50 ^ 0xFF, 0x45 ^ 0xFF, 0x20 ^ 0xFF,
    0x54 ^ 0xFF, 0x65 ^ 0xFF, 0x61 ^ 0xFF, 0x6D ^ 0xFF
};

// Deobfuscation key (constant XOR key)
static const unsigned char key_data[] = {
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
};

static std::string deobfuscate_team() {
    std::vector<char> result;
    for(size_t i = 0; i < sizeof(obfuscated_team); i++) {
        // Simple XOR deobfuscation
        char c = obfuscated_team[i] ^ key_data[i];
        result.push_back(c);
    }
    return std::string(result.begin(), result.end());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_noxpeteam_txpacker_NativeTextProvider_getTeamCreditText(
        JNIEnv* env,
        jclass /* this */) {
    return env->NewStringUTF(deobfuscate_team().c_str());
} 