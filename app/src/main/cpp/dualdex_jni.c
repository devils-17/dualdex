#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>
#include "pokemon_reader.h"
#include "pokemon_text.h"
#include "libretro_host.h"
#include "js_calc_engine.h"

#define TAG "DualDex_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static jclass g_parsed_pokemon_cls = NULL;
static jmethodID g_parsed_pokemon_ctor = NULL;

#define PARSED_POKEMON_SIG "(ZZJIILjava/lang/String;Ljava/lang/String;IIIILjava/lang/String;ZIZIJIIIIIIIIIIII[I[IIIIIIIJ)V"

static void init_class_cache(JNIEnv* env) {
    if (g_parsed_pokemon_cls != NULL && g_parsed_pokemon_ctor != NULL) return;

    if (g_parsed_pokemon_cls == NULL) {
        jclass local_cls = (*env)->FindClass(env, "com/dualdex/pokemon/ParsedPokemon");
        if (!local_cls) {
            LOGE("Failed to find com.dualdex.pokemon.ParsedPokemon");
            (*env)->ExceptionClear(env);
            return;
        }
        g_parsed_pokemon_cls = (jclass)(*env)->NewGlobalRef(env, local_cls);
        (*env)->DeleteLocalRef(env, local_cls);
    }

    if (g_parsed_pokemon_ctor == NULL && g_parsed_pokemon_cls != NULL) {
        g_parsed_pokemon_ctor = (*env)->GetMethodID(
            env,
            g_parsed_pokemon_cls,
            "<init>",
            PARSED_POKEMON_SIG
        );

        if (!g_parsed_pokemon_ctor) {
            LOGE("Failed to find ParsedPokemon constructor with signature: %s", PARSED_POKEMON_SIG);
            (*env)->ExceptionClear(env);
        } else {
            LOGI("ParsedPokemon constructor resolved successfully");
        }
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    (void)reserved;
    JNIEnv* env = NULL;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("JNI_OnLoad: GetEnv failed");
        return JNI_ERR;
    }

    init_class_cache(env);
    LOGI("DualDex JNI_OnLoad completed");
    return JNI_VERSION_1_6;
}

static jobject create_parsed_pokemon_object(JNIEnv* env, const ParsedPokemon* p) {
    if (!g_parsed_pokemon_cls || !g_parsed_pokemon_ctor) {
        init_class_cache(env);
        if (!g_parsed_pokemon_cls || !g_parsed_pokemon_ctor) return NULL;
    }

    jstring j_nickname = (*env)->NewStringUTF(env, p->nickname);
    jstring j_otname = (*env)->NewStringUTF(env, p->ot_name);
    jstring j_nature_name = (*env)->NewStringUTF(env, p->nature_name ? p->nature_name : "");

    jintArray j_moves = (*env)->NewIntArray(env, 4);
    if (!j_moves) return NULL;
    jint moves_buf[4];
    for (int i = 0; i < 4; i++) moves_buf[i] = p->moves[i];
    (*env)->SetIntArrayRegion(env, j_moves, 0, 4, moves_buf);

    jintArray j_pp = (*env)->NewIntArray(env, 4);
    if (!j_pp) { (*env)->DeleteLocalRef(env, j_moves); return NULL; }
    jint pp_buf[4];
    for (int i = 0; i < 4; i++) pp_buf[i] = p->pp[i];
    (*env)->SetIntArrayRegion(env, j_pp, 0, 4, pp_buf);

    jobject obj = (*env)->NewObject(
        env,
        g_parsed_pokemon_cls,
        g_parsed_pokemon_ctor,
        (jboolean)p->is_valid,
        (jboolean)p->is_empty,
        (jlong)p->pid,
        (jint)p->tid,
        (jint)p->sid,
        j_nickname,
        j_otname,
        (jint)p->species,
        (jint)p->held_item,
        (jint)p->level,
        (jint)p->nature,
        j_nature_name,
        (jboolean)p->is_shiny,
        (jint)p->ability_slot,
        (jboolean)p->is_egg,
        (jint)p->friendship,
        (jlong)p->experience,
        (jint)p->hp_iv,
        (jint)p->attack_iv,
        (jint)p->defense_iv,
        (jint)p->speed_iv,
        (jint)p->sp_attack_iv,
        (jint)p->sp_defense_iv,
        (jint)p->hp_ev,
        (jint)p->attack_ev,
        (jint)p->defense_ev,
        (jint)p->speed_ev,
        (jint)p->sp_attack_ev,
        (jint)p->sp_defense_ev,
        j_moves,
        j_pp,
        (jint)p->current_hp,
        (jint)p->max_hp,
        (jint)p->attack,
        (jint)p->defense,
        (jint)p->speed,
        (jint)p->sp_attack,
        (jint)p->sp_defense,
        (jlong)p->status_condition
    );

    (*env)->DeleteLocalRef(env, j_nickname);
    (*env)->DeleteLocalRef(env, j_otname);
    (*env)->DeleteLocalRef(env, j_nature_name);
    (*env)->DeleteLocalRef(env, j_moves);
    (*env)->DeleteLocalRef(env, j_pp);

    return obj;
}

// -------------------------------------------------------------
// Pokemon Memory Reader JNI
// -------------------------------------------------------------

JNIEXPORT jint JNICALL
Java_com_dualdex_pokemon_PokemonBridge_detectGame(JNIEnv* env, jobject thiz, jstring rom_title) {
    (void)thiz;
    if (!rom_title) return GAME_UNKNOWN;

    const char* title_str = (*env)->GetStringUTFChars(env, rom_title, NULL);
    GbaGameId game_id = pokemon_detect_game(title_str);
    (*env)->ReleaseStringUTFChars(env, rom_title, title_str);

    return (jint)game_id;
}

JNIEXPORT jobject JNICALL
Java_com_dualdex_pokemon_PokemonBridge_parsePokemon(
    JNIEnv* env,
    jobject thiz,
    jbyteArray raw_bytes,
    jboolean is_party_mon
) {
    (void)thiz;
    if (!raw_bytes) return NULL;

    jsize len = (*env)->GetArrayLength(env, raw_bytes);
    if (len < (is_party_mon ? 100 : 80)) return NULL;

    jbyte* bytes = (*env)->GetByteArrayElements(env, raw_bytes, NULL);
    ParsedPokemon parsed;
    bool ok = pokemon_parse_single((const uint8_t*)bytes, (bool)is_party_mon, &parsed);
    (*env)->ReleaseByteArrayElements(env, raw_bytes, bytes, JNI_ABORT);

    if (!ok && !parsed.is_empty) return NULL;

    return create_parsed_pokemon_object(env, &parsed);
}

JNIEXPORT jobjectArray JNICALL
Java_com_dualdex_pokemon_PokemonBridge_readPlayerParty(
    JNIEnv* env,
    jobject thiz,
    jbyteArray ewram_bytes,
    jint game_id
) {
    (void)thiz;
    init_class_cache(env);
    if (!g_parsed_pokemon_cls) return NULL;

    if (!ewram_bytes) {
        return (*env)->NewObjectArray(env, 0, g_parsed_pokemon_cls, NULL);
    }

    jsize len = (*env)->GetArrayLength(env, ewram_bytes);
    jbyte* bytes = (*env)->GetByteArrayElements(env, ewram_bytes, NULL);

    const GameMemoryConfig* cfg = pokemon_get_game_config((GbaGameId)game_id);
    PartySnapshot snapshot;
    uint8_t count = pokemon_read_player_party((const uint8_t*)bytes, (size_t)len, cfg, &snapshot);
    (*env)->ReleaseByteArrayElements(env, ewram_bytes, bytes, JNI_ABORT);

    jobjectArray array = (*env)->NewObjectArray(env, count, g_parsed_pokemon_cls, NULL);
    if (!array) return NULL;
    for (uint8_t i = 0; i < count; i++) {
        jobject p_obj = create_parsed_pokemon_object(env, &snapshot.members[i]);
        if (p_obj) {
            (*env)->SetObjectArrayElement(env, array, i, p_obj);
            (*env)->DeleteLocalRef(env, p_obj);
        }
    }

    return array;
}

JNIEXPORT jobjectArray JNICALL
Java_com_dualdex_pokemon_PokemonBridge_readEnemyParty(
    JNIEnv* env,
    jobject thiz,
    jbyteArray ewram_bytes,
    jint game_id
) {
    (void)thiz;
    init_class_cache(env);
    if (!g_parsed_pokemon_cls) return NULL;

    if (!ewram_bytes) {
        return (*env)->NewObjectArray(env, 0, g_parsed_pokemon_cls, NULL);
    }

    jsize len = (*env)->GetArrayLength(env, ewram_bytes);
    jbyte* bytes = (*env)->GetByteArrayElements(env, ewram_bytes, NULL);

    const GameMemoryConfig* cfg = pokemon_get_game_config((GbaGameId)game_id);
    PartySnapshot snapshot;
    uint8_t count = pokemon_read_enemy_party((const uint8_t*)bytes, (size_t)len, cfg, &snapshot);
    (*env)->ReleaseByteArrayElements(env, ewram_bytes, bytes, JNI_ABORT);

    jobjectArray array = (*env)->NewObjectArray(env, count, g_parsed_pokemon_cls, NULL);
    if (!array) return NULL;
    for (uint8_t i = 0; i < count; i++) {
        jobject p_obj = create_parsed_pokemon_object(env, &snapshot.members[i]);
        if (p_obj) {
            (*env)->SetObjectArrayElement(env, array, i, p_obj);
            (*env)->DeleteLocalRef(env, p_obj);
        }
    }

    return array;
}

// -------------------------------------------------------------
// Libretro Emulator Host JNI
// -------------------------------------------------------------

JNIEXPORT jboolean JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeLoadCore(JNIEnv* env, jobject thiz, jstring core_path) {
    (void)thiz;
    if (!core_path) return JNI_FALSE;

    const char* path_str = (*env)->GetStringUTFChars(env, core_path, NULL);
    bool ok = libretro_host_init(path_str);
    (*env)->ReleaseStringUTFChars(env, core_path, path_str);

    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeLoadRom(JNIEnv* env, jobject thiz, jstring rom_path) {
    (void)thiz;
    if (!rom_path) return JNI_FALSE;

    const char* path_str = (*env)->GetStringUTFChars(env, rom_path, NULL);
    bool ok = libretro_host_load_rom(path_str);
    (*env)->ReleaseStringUTFChars(env, rom_path, path_str);

    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeStepFrame(JNIEnv* env, jobject thiz) {
    (void)env;
    (void)thiz;
    libretro_host_step_frame();
}

JNIEXPORT void JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeSetInputButtons(JNIEnv* env, jobject thiz, jint button_mask) {
    (void)env;
    (void)thiz;
    libretro_host_set_input_buttons((uint32_t)button_mask);
}

JNIEXPORT jboolean JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeGetVideoFrame(
    JNIEnv* env,
    jobject thiz,
    jobject direct_buffer,
    jintArray out_metadata
) {
    (void)thiz;
    EmulatorVideoFrame frame;
    if (!libretro_host_get_video_frame(&frame) || !frame.pixels) {
        return JNI_FALSE;
    }

    void* dst = (*env)->GetDirectBufferAddress(env, direct_buffer);
    if (!dst) return JNI_FALSE;

    jlong dst_capacity = (*env)->GetDirectBufferCapacity(env, direct_buffer);
    size_t copy_size = (size_t)(frame.pitch * frame.height);
    if (dst_capacity < 0 || copy_size > (size_t)dst_capacity) {
        copy_size = (dst_capacity > 0) ? (size_t)dst_capacity : 0;
    }
    if (copy_size > 0) {
        memcpy(dst, frame.pixels, copy_size);
    }

    if (out_metadata) {
        jint meta[4] = {
            (jint)frame.width,
            (jint)frame.height,
            (jint)frame.pitch,
            (jint)frame.pixel_format
        };
        (*env)->SetIntArrayRegion(env, out_metadata, 0, 4, meta);
    }

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeGetAudioSamples(
    JNIEnv* env,
    jobject thiz,
    jshortArray out_buffer
) {
    (void)thiz;
    if (!out_buffer) return 0;
    jsize len = (*env)->GetArrayLength(env, out_buffer);
    jshort* buf = (*env)->GetShortArrayElements(env, out_buffer, NULL);
    size_t read = libretro_host_get_audio_samples((int16_t*)buf, (size_t)len);
    (*env)->ReleaseShortArrayElements(env, out_buffer, buf, 0);
    return (jint)read;
}

JNIEXPORT jobjectArray JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeReadPartyFromCore(JNIEnv* env, jobject thiz, jint game_id) {
    (void)thiz;
    init_class_cache(env);
    if (!g_parsed_pokemon_cls) {
        LOGE("nativeReadPartyFromCore: g_parsed_pokemon_cls is NULL!");
        return NULL;
    }

    size_t ewram_sz = 0;
    uint8_t* ewram = libretro_host_get_ewram(&ewram_sz);
    if (!ewram || ewram_sz == 0) {
        return (*env)->NewObjectArray(env, 0, g_parsed_pokemon_cls, NULL);
    }

    const GameMemoryConfig* cfg = pokemon_get_game_config((GbaGameId)game_id);
    PartySnapshot snapshot;
    uint8_t count = pokemon_read_player_party(ewram, ewram_sz, cfg, &snapshot);

    jobjectArray array = (*env)->NewObjectArray(env, count, g_parsed_pokemon_cls, NULL);
    if (!array) return NULL;
    for (uint8_t i = 0; i < count; i++) {
        jobject p_obj = create_parsed_pokemon_object(env, &snapshot.members[i]);
        if (p_obj) {
            (*env)->SetObjectArrayElement(env, array, i, p_obj);
            (*env)->DeleteLocalRef(env, p_obj);
        }
    }

    return array;
}

JNIEXPORT jobjectArray JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeReadEnemyPartyFromCore(JNIEnv* env, jobject thiz, jint game_id) {
    (void)thiz;
    init_class_cache(env);
    if (!g_parsed_pokemon_cls) {
        LOGE("nativeReadEnemyPartyFromCore: g_parsed_pokemon_cls is NULL!");
        return NULL;
    }

    size_t ewram_sz = 0;
    uint8_t* ewram = libretro_host_get_ewram(&ewram_sz);
    if (!ewram || ewram_sz == 0) {
        return (*env)->NewObjectArray(env, 0, g_parsed_pokemon_cls, NULL);
    }

    const GameMemoryConfig* cfg = pokemon_get_game_config((GbaGameId)game_id);
    PartySnapshot snapshot;
    uint8_t count = pokemon_read_enemy_party(ewram, ewram_sz, cfg, &snapshot);

    jobjectArray array = (*env)->NewObjectArray(env, count, g_parsed_pokemon_cls, NULL);
    if (!array) return NULL;
    for (uint8_t i = 0; i < count; i++) {
        jobject p_obj = create_parsed_pokemon_object(env, &snapshot.members[i]);
        if (p_obj) {
            (*env)->SetObjectArrayElement(env, array, i, p_obj);
            (*env)->DeleteLocalRef(env, p_obj);
        }
    }

    return array;
}

JNIEXPORT jboolean JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeSaveState(JNIEnv* env, jobject thiz, jstring state_path) {
    (void)thiz;
    if (!state_path) return JNI_FALSE;
    const char* path_str = (*env)->GetStringUTFChars(env, state_path, NULL);
    bool ok = libretro_host_save_state(path_str);
    (*env)->ReleaseStringUTFChars(env, state_path, path_str);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeLoadState(JNIEnv* env, jobject thiz, jstring state_path) {
    (void)thiz;
    if (!state_path) return JNI_FALSE;
    const char* path_str = (*env)->GetStringUTFChars(env, state_path, NULL);
    bool ok = libretro_host_load_state(path_str);
    (*env)->ReleaseStringUTFChars(env, state_path, path_str);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_dualdex_emulator_LibretroHost_nativeCleanup(JNIEnv* env, jobject thiz) {
    (void)env;
    (void)thiz;
    libretro_host_cleanup();
}

// -------------------------------------------------------------
// QuickJS Damage Calculator JNI
// -------------------------------------------------------------

JNIEXPORT jboolean JNICALL
Java_com_dualdex_calculator_DamageCalculator_nativeInit(JNIEnv* env, jobject thiz, jstring bundle_js) {
    (void)thiz;
    if (!bundle_js) return JNI_FALSE;

    const char* js_str = (*env)->GetStringUTFChars(env, bundle_js, NULL);
    bool ok = js_calc_init(js_str);
    (*env)->ReleaseStringUTFChars(env, bundle_js, js_str);

    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_dualdex_calculator_DamageCalculator_nativeCalculate(JNIEnv* env, jobject thiz, jstring input_json) {
    (void)thiz;
    if (!input_json) return NULL;

    const char* json_str = (*env)->GetStringUTFChars(env, input_json, NULL);
    char* result_str = js_calc_calculate(json_str);
    (*env)->ReleaseStringUTFChars(env, input_json, json_str);

    if (!result_str) return NULL;

    jstring j_out = (*env)->NewStringUTF(env, result_str);
    free(result_str);
    return j_out;
}

JNIEXPORT void JNICALL
Java_com_dualdex_calculator_DamageCalculator_nativeCleanup(JNIEnv* env, jobject thiz) {
    (void)env;
    (void)thiz;
    js_calc_cleanup();
}
