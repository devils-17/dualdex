#include "libretro_host.h"
#include "libretro.h"
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

// Core function pointers
static void (*p_retro_init)(void);
static void (*p_retro_deinit)(void);
static unsigned (*p_retro_api_version)(void);
static void (*p_retro_set_environment)(retro_environment_t);
static void (*p_retro_set_video_refresh)(retro_video_refresh_t);
static void (*p_retro_set_audio_sample)(retro_audio_sample_t);
static void (*p_retro_set_audio_sample_batch)(retro_audio_sample_batch_t);
static void (*p_retro_set_input_poll)(retro_input_poll_t);
static void (*p_retro_set_input_state)(retro_input_state_t);
static void (*p_retro_set_controller_port_device)(unsigned, unsigned);
static void (*p_retro_reset)(void);
static void (*p_retro_run)(void);
static size_t (*p_retro_serialize_size)(void);
static bool (*p_retro_serialize)(void*, size_t);
static bool (*p_retro_unserialize)(const void*, size_t);
static bool (*p_retro_load_game)(const struct retro_game_info*);
static void (*p_retro_unload_game)(void);
static void (*p_retro_get_system_info)(struct retro_system_info*);
static void (*p_retro_get_system_av_info)(struct retro_system_av_info*);
static void* (*p_retro_get_memory_data)(unsigned);
static size_t (*p_retro_get_memory_size)(unsigned);

static void* g_core_handle = NULL;
static bool g_is_game_loaded = false;
static uint32_t g_current_buttons = 0;
static enum retro_pixel_format g_pixel_format = RETRO_PIXEL_FORMAT_RGB565;

// Framebuffer storage (GBA max native resolution 240x160)
#define MAX_FB_WIDTH 512
#define MAX_FB_HEIGHT 512
static uint8_t g_fb_storage[MAX_FB_WIDTH * MAX_FB_HEIGHT * 4];
static EmulatorVideoFrame g_current_frame = {0};
static pthread_mutex_t g_video_mutex = PTHREAD_MUTEX_INITIALIZER;

// Environment callback implementation
static bool core_environment_cb(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            const enum retro_pixel_format *fmt = (const enum retro_pixel_format *)data;
            if (!fmt) return false;
            if (*fmt == RETRO_PIXEL_FORMAT_RGB565 ||
                *fmt == RETRO_PIXEL_FORMAT_XRGB8888 ||
                *fmt == RETRO_PIXEL_FORMAT_0RGB1555) {
                g_pixel_format = *fmt;
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_CAN_DUPE: {
            bool *b = (bool *)data;
            if (b) *b = true;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            return false;
        default:
            return false;
    }
}

// Video refresh callback implementation
static void core_video_refresh_cb(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!data || width == 0 || height == 0) return;

    pthread_mutex_lock(&g_video_mutex);

    size_t bpp = (g_pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) ? 4 : 2;
    size_t copy_size = width * height * bpp;
    if (copy_size > sizeof(g_fb_storage)) {
        copy_size = sizeof(g_fb_storage);
    }

    if (pitch == width * bpp) {
        memcpy(g_fb_storage, data, copy_size);
    } else {
        // Handle stride / pitch difference
        const uint8_t *src_row = (const uint8_t *)data;
        uint8_t *dst_row = g_fb_storage;
        for (unsigned y = 0; y < height; y++) {
            memcpy(dst_row, src_row, width * bpp);
            src_row += pitch;
            dst_row += (width * bpp);
        }
    }

    g_current_frame.width = width;
    g_current_frame.height = height;
    g_current_frame.pixel_format = (int)g_pixel_format;
    g_current_frame.pitch = width * bpp;
    g_current_frame.pixels = g_fb_storage;

    pthread_mutex_unlock(&g_video_mutex);
}

// Audio ring buffer (supports 44100Hz stereo PCM)
#define AUDIO_RING_BUFFER_SIZE (65536)
static int16_t g_audio_ring_buffer[AUDIO_RING_BUFFER_SIZE];
static size_t g_audio_write_pos = 0;
static size_t g_audio_read_pos = 0;
static pthread_mutex_t g_audio_mutex = PTHREAD_MUTEX_INITIALIZER;

// Audio sample callbacks
static void core_audio_sample_cb(int16_t left, int16_t right) {
    pthread_mutex_lock(&g_audio_mutex);
    g_audio_ring_buffer[g_audio_write_pos] = left;
    g_audio_write_pos = (g_audio_write_pos + 1) % AUDIO_RING_BUFFER_SIZE;
    g_audio_ring_buffer[g_audio_write_pos] = right;
    g_audio_write_pos = (g_audio_write_pos + 1) % AUDIO_RING_BUFFER_SIZE;
    pthread_mutex_unlock(&g_audio_mutex);
}

static size_t core_audio_sample_batch_cb(const int16_t *data, size_t frames) {
    if (!data || frames == 0) return 0;

    pthread_mutex_lock(&g_audio_mutex);
    size_t count = frames * 2;
    for (size_t i = 0; i < count; i++) {
        g_audio_ring_buffer[g_audio_write_pos] = data[i];
        g_audio_write_pos = (g_audio_write_pos + 1) % AUDIO_RING_BUFFER_SIZE;
    }
    pthread_mutex_unlock(&g_audio_mutex);
    return frames;
}

size_t libretro_host_get_audio_samples(int16_t* out_buffer, size_t max_samples) {
    if (!out_buffer || max_samples == 0) return 0;

    pthread_mutex_lock(&g_audio_mutex);
    size_t available = (g_audio_write_pos >= g_audio_read_pos)
        ? (g_audio_write_pos - g_audio_read_pos)
        : (AUDIO_RING_BUFFER_SIZE - g_audio_read_pos + g_audio_write_pos);

    size_t to_read = (available < max_samples) ? available : max_samples;
    for (size_t i = 0; i < to_read; i++) {
        out_buffer[i] = g_audio_ring_buffer[g_audio_read_pos];
        g_audio_read_pos = (g_audio_read_pos + 1) % AUDIO_RING_BUFFER_SIZE;
    }
    pthread_mutex_unlock(&g_audio_mutex);
    return to_read;
}

// Input poll and state callbacks
static void core_input_poll_cb(void) {
    // No-op, polled externally
}

static int16_t core_input_state_cb(unsigned port, unsigned device, unsigned index, unsigned id) {
    (void)index;
    if (port != 0 || device != RETRO_DEVICE_JOYPAD) return 0;

    switch (id) {
        case RETRO_DEVICE_ID_JOYPAD_B:      return (g_current_buttons & DUALDEX_BTN_B) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_Y:      return (g_current_buttons & DUALDEX_BTN_Y) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_SELECT: return (g_current_buttons & DUALDEX_BTN_SELECT) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_START:  return (g_current_buttons & DUALDEX_BTN_START) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_UP:     return (g_current_buttons & DUALDEX_BTN_UP) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_DOWN:   return (g_current_buttons & DUALDEX_BTN_DOWN) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_LEFT:   return (g_current_buttons & DUALDEX_BTN_LEFT) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_RIGHT:  return (g_current_buttons & DUALDEX_BTN_RIGHT) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_A:      return (g_current_buttons & DUALDEX_BTN_A) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_X:      return (g_current_buttons & DUALDEX_BTN_X) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_L:      return (g_current_buttons & DUALDEX_BTN_L) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_R:      return (g_current_buttons & DUALDEX_BTN_R) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_L2:     return (g_current_buttons & DUALDEX_BTN_L2) ? 1 : 0;
        case RETRO_DEVICE_ID_JOYPAD_R2:     return (g_current_buttons & DUALDEX_BTN_R2) ? 1 : 0;
        default: return 0;
    }
}

#define RESOLVE_SYM(name) do { \
    p_##name = dlsym(g_core_handle, #name); \
    if (!p_##name) { \
        fprintf(stderr, "Failed to resolve Libretro symbol: %s\n", #name); \
        dlclose(g_core_handle); \
        g_core_handle = NULL; \
        return false; \
    } \
} while(0)

bool libretro_host_init(const char* core_lib_path) {
    if (!core_lib_path) return false;
    if (g_core_handle) return true;

    g_core_handle = dlopen(core_lib_path, RTLD_LAZY | RTLD_LOCAL);
    if (!g_core_handle) {
        fprintf(stderr, "Failed to dlopen core library: %s (error: %s)\n", core_lib_path, dlerror());
        return false;
    }

    RESOLVE_SYM(retro_init);
    RESOLVE_SYM(retro_deinit);
    RESOLVE_SYM(retro_api_version);
    RESOLVE_SYM(retro_set_environment);
    RESOLVE_SYM(retro_set_video_refresh);
    RESOLVE_SYM(retro_set_audio_sample);
    RESOLVE_SYM(retro_set_audio_sample_batch);
    RESOLVE_SYM(retro_set_input_poll);
    RESOLVE_SYM(retro_set_input_state);
    RESOLVE_SYM(retro_set_controller_port_device);
    RESOLVE_SYM(retro_reset);
    RESOLVE_SYM(retro_run);
    RESOLVE_SYM(retro_serialize_size);
    RESOLVE_SYM(retro_serialize);
    RESOLVE_SYM(retro_unserialize);
    RESOLVE_SYM(retro_load_game);
    RESOLVE_SYM(retro_unload_game);
    RESOLVE_SYM(retro_get_system_info);
    RESOLVE_SYM(retro_get_system_av_info);
    RESOLVE_SYM(retro_get_memory_data);
    RESOLVE_SYM(retro_get_memory_size);

    // Initialize core
    p_retro_set_environment(core_environment_cb);
    p_retro_set_video_refresh(core_video_refresh_cb);
    p_retro_set_audio_sample(core_audio_sample_cb);
    p_retro_set_audio_sample_batch(core_audio_sample_batch_cb);
    p_retro_set_input_poll(core_input_poll_cb);
    p_retro_set_input_state(core_input_state_cb);

    p_retro_init();
    return true;
}

bool libretro_host_load_rom(const char* rom_file_path) {
    if (!g_core_handle || !rom_file_path) return false;

    FILE* f = fopen(rom_file_path, "rb");
    if (!f) {
        fprintf(stderr, "Failed to open ROM file: %s\n", rom_file_path);
        return false;
    }

    fseek(f, 0, SEEK_END);
    size_t size = ftell(f);
    fseek(f, 0, SEEK_SET);

    void* data = malloc(size);
    if (!data) {
        fclose(f);
        return false;
    }
    size_t rd = fread(data, 1, size, f);
    fclose(f);

    if (rd != size) {
        free(data);
        return false;
    }

    struct retro_game_info game_info;
    memset(&game_info, 0, sizeof(game_info));
    game_info.path = rom_file_path;
    game_info.data = data;
    game_info.size = size;

    bool ok = p_retro_load_game(&game_info);
    free(data);

    if (ok) {
        g_is_game_loaded = true;
        p_retro_set_controller_port_device(0, RETRO_DEVICE_JOYPAD);
    }
    return ok;
}

void libretro_host_step_frame(void) {
    if (g_core_handle && g_is_game_loaded && p_retro_run) {
        p_retro_run();
    }
}

bool libretro_host_get_video_frame(EmulatorVideoFrame* out_frame) {
    if (!out_frame) return false;

    pthread_mutex_lock(&g_video_mutex);
    *out_frame = g_current_frame;
    pthread_mutex_unlock(&g_video_mutex);

    return (out_frame->pixels != NULL);
}

void libretro_host_set_input_buttons(uint32_t button_mask) {
    g_current_buttons = button_mask;
}

uint8_t* libretro_host_get_ewram(size_t* out_size) {
    if (!g_core_handle || !g_is_game_loaded || !p_retro_get_memory_data) {
        if (out_size) *out_size = 0;
        return NULL;
    }

    void* ptr = p_retro_get_memory_data(RETRO_MEMORY_SYSTEM_RAM);
    size_t sz = p_retro_get_memory_size(RETRO_MEMORY_SYSTEM_RAM);

    if (out_size) *out_size = sz;
    return (uint8_t*)ptr;
}

bool libretro_host_save_state(const char* save_state_path) {
    if (!g_core_handle || !g_is_game_loaded || !save_state_path || !p_retro_serialize_size || !p_retro_serialize) {
        return false;
    }

    size_t sz = p_retro_serialize_size();
    if (sz == 0) return false;

    void* buf = malloc(sz);
    if (!buf) return false;

    bool ok = p_retro_serialize(buf, sz);
    if (ok) {
        FILE* f = fopen(save_state_path, "wb");
        if (f) {
            fwrite(buf, 1, sz, f);
            fclose(f);
        } else {
            ok = false;
        }
    }
    free(buf);
    return ok;
}

bool libretro_host_load_state(const char* save_state_path) {
    if (!g_core_handle || !g_is_game_loaded || !save_state_path || !p_retro_unserialize) {
        return false;
    }

    FILE* f = fopen(save_state_path, "rb");
    if (!f) return false;

    fseek(f, 0, SEEK_END);
    size_t sz = ftell(f);
    fseek(f, 0, SEEK_SET);

    void* buf = malloc(sz);
    if (!buf) {
        fclose(f);
        return false;
    }
    size_t rd = fread(buf, 1, sz, f);
    fclose(f);

    bool ok = false;
    if (rd == sz) {
        ok = p_retro_unserialize(buf, sz);
    }
    free(buf);
    return ok;
}

void libretro_host_cleanup(void) {
    if (g_is_game_loaded && p_retro_unload_game) {
        p_retro_unload_game();
        g_is_game_loaded = false;
    }
    if (g_core_handle && p_retro_deinit) {
        p_retro_deinit();
    }
    if (g_core_handle) {
        dlclose(g_core_handle);
        g_core_handle = NULL;
    }
}
