#ifndef DUALDEX_POKEMON_TEXT_H
#define DUALDEX_POKEMON_TEXT_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Decode Gen 3 character encoded string to null-terminated UTF-8.
 *
 * @param src Pointer to raw Gen 3 bytes
 * @param src_len Maximum length of src buffer
 * @param dst Destination buffer for UTF-8 string
 * @param dst_size Size of destination buffer
 * @return Number of characters written (excluding null terminator)
 */
size_t pokemon_decode_string(const uint8_t* src, size_t src_len, char* dst, size_t dst_size);

#ifdef __cplusplus
}
#endif

#endif // DUALDEX_POKEMON_TEXT_H
