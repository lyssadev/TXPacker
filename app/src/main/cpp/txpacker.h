#pragma once

#include <jni.h>
#include <string>
#include <vector>
#include <cstdint>

// Prevent name mangling for C++
extern "C" {
    // Basic optimization functions
    JNIEXPORT jboolean JNICALL
    Java_com_noxpeteam_txpacker_TXPackerLib_isOptimizationAvailable(JNIEnv *env, jobject thiz);

    JNIEXPORT jint JNICALL
    Java_com_noxpeteam_txpacker_TXPackerLib_getOptimizationLevel(JNIEnv *env, jobject thiz);

    JNIEXPORT void JNICALL
    Java_com_noxpeteam_txpacker_TXPackerLib_setOptimizationLevel(JNIEnv *env, jobject thiz, jint level);
}

// Internal optimization functions
namespace txpacker {
    // Check if optimizations are supported on the current device
    bool checkOptimizationSupport();
    
    // Get current optimization level
    int getCurrentOptimizationLevel();
    
    // Set optimization level and initialize required resources
    void setOptimizationLevel(int level);
    
    // Process texture data with optimizations (auto-detects device capabilities)
    void processTextureData(uint8_t* data, size_t size);
    
    // Memory management functions
    void initMemoryPool();
    void cleanupMemoryPool();
} 