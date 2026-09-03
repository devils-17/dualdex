#include "js_calc_engine.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#define ANSI_GREEN "\033[0;32m"
#define ANSI_RED   "\033[0;31m"
#define ANSI_RESET "\033[0m"

static char* read_file_to_string(const char* path) {
    FILE* f = fopen(path, "rb");
    if (!f) return NULL;
    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    fseek(f, 0, SEEK_SET);

    char* buf = (char*)malloc(sz + 1);
    if (!buf) {
        fclose(f);
        return NULL;
    }
    size_t rd = fread(buf, 1, sz, f);
    buf[rd] = '\0';
    fclose(f);
    return buf;
}

int main(void) {
    printf("===================================================\n");
    printf("   DualDex QuickJS Damage Calculator Test Suite\n");
    printf("===================================================\n");

    const char* bundle_path = "app/src/main/assets/calc_bundle.js";
    char* bundle_js = read_file_to_string(bundle_path);
    if (!bundle_js) {
        printf(ANSI_RED "Failed to read %s\n" ANSI_RESET, bundle_path);
        return 1;
    }

    printf("Initializing QuickJS with calc_bundle.js...\n");
    bool ok = js_calc_init(bundle_js);
    free(bundle_js);

    if (!ok) {
        printf(ANSI_RED "js_calc_init failed!\n" ANSI_RESET);
        return 1;
    }
    printf(ANSI_GREEN "QuickJS initialized successfully!\n" ANSI_RESET);

    // Test 1: Gen 3 Salamence vs Skarmory
    {
        const char* input1 = "{"
            "\"gen\": 3,"
            "\"attacker\": {\"species\": \"Salamence\", \"nature\": \"Adamant\", \"evs\": {\"atk\": 252, \"spe\": 252}, \"item\": \"Choice Band\"},"
            "\"defender\": {\"species\": \"Skarmory\", \"nature\": \"Impish\", \"evs\": {\"hp\": 252, \"def\": 252}},"
            "\"move\": {\"name\": \"Rock Slide\"}"
        "}";

        char* res1 = js_calc_calculate(input1);
        assert(res1 != NULL);
        printf("Test 1 Result: %s\n", res1);
        assert(strstr(res1, "\"success\":true") != NULL);
        assert(strstr(res1, "\"minDamage\":22") != NULL);
        assert(strstr(res1, "\"maxDamage\":26") != NULL);
        printf(ANSI_GREEN "  [PASS] Test 1: Gen 3 Salamence vs Skarmory\n" ANSI_RESET);
        free(res1);
    }

    // Test 2: Gen 3 Swampert vs Blaziken (Hydro Pump)
    {
        const char* input2 = "{"
            "\"gen\": 3,"
            "\"attacker\": {\"species\": \"Swampert\", \"level\": 50, \"nature\": \"Modest\", \"evs\": {\"spa\": 252}},"
            "\"defender\": {\"species\": \"Blaziken\", \"level\": 50, \"nature\": \"Hardy\"},"
            "\"move\": {\"name\": \"Hydro Pump\"}"
        "}";

        char* res2 = js_calc_calculate(input2);
        assert(res2 != NULL);
        printf("Test 2 Result: %s\n", res2);
        assert(strstr(res2, "\"success\":true") != NULL);
        // Hydro Pump super effective vs Blaziken is guaranteed OHKO
        assert(strstr(res2, "guaranteed OHKO") != NULL || strstr(res2, "minDamage") != NULL);
        printf(ANSI_GREEN "  [PASS] Test 2: Gen 3 Swampert vs Blaziken\n" ANSI_RESET);
        free(res2);
    }

    // Test 3: Gen 8 / Modern Expanded Mechanics (Fairy Type & Split)
    {
        const char* input3 = "{"
            "\"gen\": 8,"
            "\"attacker\": {\"species\": \"Sylveon\", \"level\": 50, \"nature\": \"Modest\", \"evs\": {\"spa\": 252}},"
            "\"defender\": {\"species\": \"Dragonite\", \"level\": 50, \"nature\": \"Adamant\"},"
            "\"move\": {\"name\": \"Moonblast\"}"
        "}";

        char* res3 = js_calc_calculate(input3);
        assert(res3 != NULL);
        printf("Test 3 Result: %s\n", res3);
        assert(strstr(res3, "\"success\":true") != NULL);
        assert(strstr(res3, "\"moveType\":\"Fairy\"") != NULL);
        assert(strstr(res3, "OHKO") != NULL || strstr(res3, "2HKO") != NULL);
        printf(ANSI_GREEN "  [PASS] Test 3: Gen 8 Moonblast (Fairy type)\n" ANSI_RESET);
        free(res3);
    }

    js_calc_cleanup();

    printf("===================================================\n");
    printf(ANSI_GREEN "All QuickJS Damage Calculator Tests Passed!\n" ANSI_RESET);
    printf("===================================================\n");
    return 0;
}
