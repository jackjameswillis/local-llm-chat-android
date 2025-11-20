#include <jni.h>
#include <string>
#include <android/log.h>

static bool g_loaded = false;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_localllmchat_model_inference_LlamaCppModelRunner_nativeInit(
        JNIEnv *env, jobject thiz, jstring path_, jint ctx, jint gpuLayers) {
    const char *path = env->GetStringUTFChars(path_, 0);
    __android_log_print(ANDROID_LOG_INFO, "llama_jni", "Loading model: %s ctx=%d gpuLayers=%d", path, ctx, gpuLayers);
    g_loaded = true;
    env->ReleaseStringUTFChars(path_, path);
    return (jboolean) g_loaded;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_localllmchat_model_inference_LlamaCppModelRunner_nativeGenerateToken(
        JNIEnv *env, jobject thiz, jstring prompt_) {
    if(!g_loaded) return env->NewStringUTF("");
    static int counter = 0;
    if(counter++ > 50) {
        counter = 0;
        return nullptr;
    }
    return env->NewStringUTF(" token");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_localllmchat_model_inference_LlamaCppModelRunner_nativeRelease(
        JNIEnv *env, jobject thiz) {
    g_loaded = false;
    __android_log_print(ANDROID_LOG_INFO, "llama_jni", "Model released");
}
