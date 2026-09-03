#include "pokemon_text.h"
#include <string.h>

size_t pokemon_decode_string(const uint8_t* src, size_t src_len, char* dst, size_t dst_size) {
    if (!src || !dst || dst_size == 0) return 0;

    size_t out_idx = 0;
    for (size_t i = 0; i < src_len; i++) {
        uint8_t c = src[i];
        if (c == 0xFF) {
            // Terminator
            break;
        }

        if (out_idx + 4 >= dst_size) {
            // Prevent buffer overflow
            break;
        }

        if (c == 0x00) {
            dst[out_idx++] = ' ';
        } else if (c >= 0xA1 && c <= 0xAA) {
            // Numbers '0' - '9'
            dst[out_idx++] = (char)('0' + (c - 0xA1));
        } else if (c == 0xAB) {
            dst[out_idx++] = '!';
        } else if (c == 0xAC) {
            dst[out_idx++] = '?';
        } else if (c == 0xAD) {
            dst[out_idx++] = '.';
        } else if (c == 0xAE) {
            dst[out_idx++] = '-';
        } else if (c == 0xAF) {
            dst[out_idx++] = '.';
        } else if (c == 0xB0) {
            // Ellipsis "..."
            if (out_idx + 3 < dst_size) {
                dst[out_idx++] = '.';
                dst[out_idx++] = '.';
                dst[out_idx++] = '.';
            }
        } else if (c == 0xB1 || c == 0xB2) {
            dst[out_idx++] = '"';
        } else if (c == 0xB3 || c == 0xB4) {
            dst[out_idx++] = '\'';
        } else if (c == 0xB5) {
            // Male symbol ♂ (UTF-8: 0xE2 0x99 0x82)
            if (out_idx + 3 < dst_size) {
                dst[out_idx++] = (char)0xE2;
                dst[out_idx++] = (char)0x99;
                dst[out_idx++] = (char)0x82;
            }
        } else if (c == 0xB6) {
            // Female symbol ♀ (UTF-8: 0xE2 0x99 0x80)
            if (out_idx + 3 < dst_size) {
                dst[out_idx++] = (char)0xE2;
                dst[out_idx++] = (char)0x99;
                dst[out_idx++] = (char)0x80;
            }
        } else if (c == 0xB8) {
            dst[out_idx++] = ',';
        } else if (c == 0xBA) {
            dst[out_idx++] = '/';
        } else if (c >= 0xBB && c <= 0xD4) {
            // Uppercase 'A' - 'Z'
            dst[out_idx++] = (char)('A' + (c - 0xBB));
        } else if (c >= 0xD5 && c <= 0xEE) {
            // Lowercase 'a' - 'z'
            dst[out_idx++] = (char)('a' + (c - 0xD5));
        } else {
            // Unknown or unhandled symbol, write '?'
            dst[out_idx++] = '?';
        }
    }

    dst[out_idx] = '\0';
    return out_idx;
}
