#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>
#include "pokemon_reader.h"
#include "pokemon_text.h"

#define TAG "DualDex_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static jclass g_parsed_pokemon_cls = NULL;
static jmethodID g_parsed_pokemon_ctor = NULL;

static void init_class_cache(JNIEnv* env) {
    if (g_parsed_pokemon_cls != NULL) return;

    jclass local_cls = (*env)->FindClass(env, "com/dualdex/pokemon/ParsedPokemon");
    if (!local_cls) {
        LOGE("Failed to find com.dualdex.pokemon.ParsedPokemon");
        return;
    }
    g_parsed_pokemon_cls = (jclass)(*env)->NewGlobalRef(env, local_cls);
    (*env)->DeleteLocalRef(env, local_cls);

    g_parsed_pokemon_ctor = (*env)->GetMethodID(
        env,
        g_parsed_pokemon_cls,
        "<init>",
        "(ZZJII"                           // isValid, isEmpty, pid, tid, sid
        "Ljava/lang/String;Ljava/lang/String;" // nickname, otName
        "IIIII"                            // species, heldItem, level, nature, natureName... wait natureName is string
        "Ljava/lang/String;"               // natureName
        "ZIZJ"                             // isShiny, abilitySlot, isEgg, friendship, experience
        "IIIIII"                           // hpIv, attackIv, defenseIv, speedIv, spAttackIv, spDefenseIv
        "IIIIII"                           // hpEv, attackEv, defenseEv, speedEv, spAttackEv, spDefenseEv
        "[I[I"                             // moves, pp
        "IIIIIIJ)V"                        // currentHp, maxHp, attack, defense, speed, spAttack, spDefense, statusCondition
    );

    if (!g_parsed_pokemon_ctor) {
        LOGE("Failed to find ParsedPokemon constructor");
    }
}

static jobject create_parsed_pokemon_object(JNIEnv* env, const ParsedPokemon* p) {
    if (!g_parsed_pokemon_cls || !g_parsed_pokemon_ctor) {
        init_class_cache(env);
        if (!g_parsed_pokemon_cls || !g_parsed_pokemon_ctor) return NULL;
    }

    jstring j_nickname = (*env)->NewStringUTF(env, p->nickname);
    jstring j_otname = (*env)->NewStringUTF(env, p->ot_name);
    jstring j_nature_name = (*env)->NewStringUTF(env, p->nature_name ? p->nature_name : "");

    // Moves int array
    jintArray j_moves = (*env)->NewIntArray(env, 4);
    jint moves_buf[4];
    for (int i = 0; i < 4; i++) moves_buf[i] = p->moves[i];
    (*env)->SetIntArrayRegion(env, j_moves, 0, 4, moves_buf);

    // PP int array
    jintArray j_pp = (*env)->NewIntArray(env, 4);
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
    for (uint8_t i = 0; i < count; i++) {
        jobject p_obj = create_parsed_pokemon_object(env, &snapshot.members[i]);
        (*env)->SetObjectArrayElement(env, array, i, p_obj);
        (*env)->DeleteLocalRef(env, p_obj);
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
    for (uint8_t i = 0; i < count; i++) {
        jobject p_obj = create_parsed_pokemon_object(env, &snapshot.members[i]);
        (*env)->SetObjectArrayElement(env, array, i, p_obj);
        (*env)->DeleteLocalRef(env, p_obj);
    }

    return array;
}
