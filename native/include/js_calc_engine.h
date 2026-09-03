#ifndef DUALDEX_JS_CALC_ENGINE_H
#define DUALDEX_JS_CALC_ENGINE_H

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize QuickJS runtime and evaluate the bundled @smogon/calc engine.
 *
 * @param bundle_js_code Full JavaScript source of calc_bundle.js
 * @return True on successful initialization and bundle evaluation
 */
bool js_calc_init(const char* bundle_js_code);

/**
 * Run a damage calculation.
 *
 * @param input_json_str JSON string containing attacker, defender, move, field, gen
 * @return Newly allocated JSON response string (caller must free with free()), or NULL on error
 */
char* js_calc_calculate(const char* input_json_str);

/**
 * Clean up the QuickJS runtime and context.
 */
void js_calc_cleanup(void);

#ifdef __cplusplus
}
#endif

#endif // DUALDEX_JS_CALC_ENGINE_H
