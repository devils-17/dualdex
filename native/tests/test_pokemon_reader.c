#include "pokemon_reader.h"
#include "pokemon_text.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#define ANSI_GREEN "\033[0;32m"
#define ANSI_RED   "\033[0;31m"
#define ANSI_RESET "\033[0m"

static int g_tests_passed = 0;
static int g_tests_failed = 0;

#define TEST_ASSERT(cond, msg) do { \
    if (!(cond)) { \
        printf(ANSI_RED "  [FAIL] %s: line %d (%s)" ANSI_RESET "\n", msg, __LINE__, #cond); \
        g_tests_failed++; \
        return; \
    } \
} while (0)

// Helper: Encrypt 48 bytes of substructures and compute checksum
static void pack_and_encrypt(
    uint32_t pid,
    uint32_t otid,
    const uint8_t* g_block,
    const uint8_t* a_block,
    const uint8_t* e_block,
    const uint8_t* m_block,
    uint8_t* out_encrypted,
    uint16_t* out_checksum
) {
    uint32_t order = pid % 24;
    // Map of GAEM blocks into 4 slots
    static const char* const ORDERS[24] = {
        "GAEM", "GAME", "GEAM", "GEMA", "GMAE", "GMEA",
        "AGEM", "AGME", "AEGM", "AEMG", "AMGE", "AMEG",
        "EGAM", "EGMA", "EAGM", "EAMG", "EMGA", "EMAG",
        "MGAE", "MGEA", "MAGE", "MAEG", "MEGA", "MEAG"
    };

    uint8_t plain[48];
    const char* o = ORDERS[order];
    for (int slot = 0; slot < 4; slot++) {
        const uint8_t* src = NULL;
        switch (o[slot]) {
            case 'G': src = g_block; break;
            case 'A': src = a_block; break;
            case 'E': src = e_block; break;
            case 'M': src = m_block; break;
        }
        memcpy(plain + (slot * 12), src, 12);
    }

    // 16-bit Checksum over 24 half-words
    uint16_t sum = 0;
    const uint16_t* hwords = (const uint16_t*)plain;
    for (int i = 0; i < 24; i++) {
        sum += hwords[i];
    }
    *out_checksum = sum;

    // XOR encrypt with key = pid ^ otid
    uint32_t key = pid ^ otid;
    const uint32_t* pwords = (const uint32_t*)plain;
    uint32_t* ewords = (uint32_t*)out_encrypted;
    for (int i = 0; i < 12; i++) {
        ewords[i] = pwords[i] ^ key;
    }
}

static void test_text_decoding(void) {
    printf("Running test_text_decoding...\n");

    // Gen 3 encoding for "PIKA" + terminator
    // P = 0xCA, I = 0xC3, K = 0xC5, A = 0xBB, 0xFF = end
    uint8_t raw_pika[5] = {0xCA, 0xC3, 0xC5, 0xBB, 0xFF};
    char decoded[32];
    size_t len = pokemon_decode_string(raw_pika, sizeof(raw_pika), decoded, sizeof(decoded));

    TEST_ASSERT(len == 4, "Decoded string length mismatch");
    TEST_ASSERT(strcmp(decoded, "PIKA") == 0, "Decoded content mismatch for PIKA");

    // Test numbers, punctuation and special symbols
    // '0' = 0xA1, '9' = 0xAA, '!' = 0xAB, '?' = 0xAC, space = 0x00, male = 0xB5
    uint8_t raw_symbols[] = {0xA1, 0xAA, 0x00, 0xAB, 0xAC, 0xB5, 0xFF};
    pokemon_decode_string(raw_symbols, sizeof(raw_symbols), decoded, sizeof(decoded));
    TEST_ASSERT(strstr(decoded, "09 !?♂") != NULL, "Special symbols and male icon decoded");

    g_tests_passed++;
    printf(ANSI_GREEN "  [PASS] test_text_decoding" ANSI_RESET "\n");
}

static void test_single_pokemon_decryption(void) {
    printf("Running test_single_pokemon_decryption...\n");

    RawGbaPokemon raw;
    memset(&raw, 0, sizeof(raw));

    raw.pid = 0x87654321;
    raw.otid = 0x12345678;

    // Nickname: "TORCHIC" in Gen 3 encoding
    // T=0xCE, O=0xC9, R=0xCC, C=0xBD, H=0xC2, I=0xC3, C=0xBD, 0xFF
    uint8_t nick_encoded[10] = {0xCE, 0xC9, 0xCC, 0xBD, 0xC2, 0xC3, 0xBD, 0xFF, 0x00, 0x00};
    memcpy(raw.nickname, nick_encoded, 10);

    // Prepare substructures
    SubstructGrowth g;
    memset(&g, 0, sizeof(g));
    g.species = 255; // Torchic
    g.held_item = 15; // e.g. Oran Berry
    g.experience = 125000;
    g.friendship = 200;

    SubstructAttacks a;
    memset(&a, 0, sizeof(a));
    a.moves[0] = 52;  // Ember
    a.moves[1] = 10;  // Scratch
    a.moves[2] = 28;  // Sand Attack
    a.moves[3] = 45;  // Growl
    a.pp[0] = 25; a.pp[1] = 35; a.pp[2] = 15; a.pp[3] = 40;

    SubstructEVs e;
    memset(&e, 0, sizeof(e));
    e.hp_ev = 12;
    e.attack_ev = 120;
    e.defense_ev = 40;
    e.speed_ev = 85;
    e.sp_attack_ev = 50;
    e.sp_defense_ev = 30;

    SubstructMisc m;
    memset(&m, 0, sizeof(m));
    // IV bitfield: HP=31, Atk=28, Def=15, Spe=30, SpA=25, SpD=20, isEgg=0, ability=1
    uint32_t ivs = (31 << 0) | (28 << 5) | (15 << 10) | (30 << 15) | (25 << 20) | (20 << 25) | (0 << 30) | (1U << 31);
    m.iv_egg_ability = ivs;

    // Encrypt
    pack_and_encrypt(raw.pid, raw.otid,
                     (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                     raw.raw_substructures, &raw.checksum);

    // Battle stats
    raw.level = 36;
    raw.current_hp = 88;
    raw.max_hp = 95;
    raw.attack = 72;
    raw.defense = 48;
    raw.speed = 65;
    raw.sp_attack = 55;
    raw.sp_defense = 45;

    // Parse
    ParsedPokemon parsed;
    bool ok = pokemon_parse_single((const uint8_t*)&raw, true, &parsed);

    TEST_ASSERT(ok, "pokemon_parse_single should succeed");
    TEST_ASSERT(parsed.is_valid, "parsed.is_valid should be true");
    TEST_ASSERT(strcmp(parsed.nickname, "TORCHIC") == 0, "Nickname should match TORCHIC");
    TEST_ASSERT(parsed.species == 255, "Species should be 255 (Torchic)");
    TEST_ASSERT(parsed.held_item == 15, "Held item should be 15");
    TEST_ASSERT(parsed.experience == 125000, "EXP should match");
    TEST_ASSERT(parsed.friendship == 200, "Friendship should match");

    TEST_ASSERT(parsed.moves[0] == 52 && parsed.pp[0] == 25, "Move 1 should be Ember (52)");
    TEST_ASSERT(parsed.moves[1] == 10 && parsed.pp[1] == 35, "Move 2 should be Scratch (10)");

    TEST_ASSERT(parsed.hp_ev == 12, "HP EV should be 12");
    TEST_ASSERT(parsed.attack_ev == 120, "Atk EV should be 120");
    TEST_ASSERT(parsed.speed_ev == 85, "Spe EV should be 85");

    TEST_ASSERT(parsed.hp_iv == 31, "HP IV should be 31");
    TEST_ASSERT(parsed.attack_iv == 28, "Atk IV should be 28");
    TEST_ASSERT(parsed.defense_iv == 15, "Def IV should be 15");
    TEST_ASSERT(parsed.speed_iv == 30, "Spe IV should be 30");
    TEST_ASSERT(parsed.sp_attack_iv == 25, "SpA IV should be 25");
    TEST_ASSERT(parsed.sp_defense_iv == 20, "SpD IV should be 20");
    TEST_ASSERT(!parsed.is_egg, "is_egg should be false");
    TEST_ASSERT(parsed.ability_slot == 1, "ability_slot should be 1");

    TEST_ASSERT(parsed.level == 36, "Level should be 36");
    TEST_ASSERT(parsed.current_hp == 88, "Current HP should be 88");
    TEST_ASSERT(parsed.max_hp == 95, "Max HP should be 95");

    uint8_t expected_nature = (uint8_t)(raw.pid % 25);
    TEST_ASSERT(parsed.nature == expected_nature, "Nature index matches PID % 25");
    TEST_ASSERT(strcmp(parsed.nature_name, pokemon_get_nature_name(expected_nature)) == 0, "Nature name matches");

    g_tests_passed++;
    printf(ANSI_GREEN "  [PASS] test_single_pokemon_decryption" ANSI_RESET "\n");
}

static void test_all_substructure_orders(void) {
    printf("Running test_all_substructure_orders (verifying all 24 permutations)...\n");

    SubstructGrowth g = {.species = 384, .held_item = 0, .experience = 1000000, .friendship = 100};
    SubstructAttacks a = {.moves = {1, 2, 3, 4}, .pp = {10, 20, 30, 40}};
    SubstructEVs e = {.hp_ev = 10, .attack_ev = 20, .defense_ev = 30, .speed_ev = 40, .sp_attack_ev = 50, .sp_defense_ev = 60};
    SubstructMisc m = {.iv_egg_ability = (31 << 0) | (31 << 5) | (31 << 10) | (31 << 15) | (31 << 20) | (31 << 25)};

    // Loop through all 24 permutations (order 0 to 23)
    for (uint32_t target_order = 0; target_order < 24; target_order++) {
        RawGbaPokemon raw;
        memset(&raw, 0, sizeof(raw));

        // Ensure (pid % 24) == target_order
        raw.pid = (100 * 24) + target_order;
        raw.otid = 0x98765432;

        pack_and_encrypt(raw.pid, raw.otid,
                         (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                         raw.raw_substructures, &raw.checksum);

        ParsedPokemon parsed;
        bool ok = pokemon_parse_single((const uint8_t*)&raw, false, &parsed);

        char fail_msg[64];
        snprintf(fail_msg, sizeof(fail_msg), "Permutation order %u failed", target_order);
        TEST_ASSERT(ok, fail_msg);
        TEST_ASSERT(parsed.species == 384, fail_msg);
        TEST_ASSERT(parsed.moves[0] == 1 && parsed.moves[3] == 4, fail_msg);
        TEST_ASSERT(parsed.hp_ev == 10 && parsed.sp_defense_ev == 60, fail_msg);
        TEST_ASSERT(parsed.hp_iv == 31 && parsed.sp_defense_iv == 31, fail_msg);
    }

    g_tests_passed++;
    printf(ANSI_GREEN "  [PASS] test_all_substructure_orders (all 24 orders verified)" ANSI_RESET "\n");
}

static void test_checksum_corruption_detection(void) {
    printf("Running test_checksum_corruption_detection...\n");

    RawGbaPokemon raw;
    memset(&raw, 0, sizeof(raw));
    raw.pid = 0x11223344;
    raw.otid = 0x55667788;

    SubstructGrowth g = {.species = 1};
    SubstructAttacks a = {0};
    SubstructEVs e = {0};
    SubstructMisc m = {0};

    pack_and_encrypt(raw.pid, raw.otid,
                     (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                     raw.raw_substructures, &raw.checksum);

    // Corrupt one byte of encrypted data
    raw.raw_substructures[7] ^= 0x01;

    ParsedPokemon parsed;
    bool ok = pokemon_parse_single((const uint8_t*)&raw, false, &parsed);

    TEST_ASSERT(!ok, "Corrupted substructure must fail validation");
    TEST_ASSERT(!parsed.is_valid, "is_valid must be false on corrupted checksum");

    g_tests_passed++;
    printf(ANSI_GREEN "  [PASS] test_checksum_corruption_detection" ANSI_RESET "\n");
}

static void test_shininess_calculation(void) {
    printf("Running test_shininess_calculation...\n");

    // Shininess formula: ((TID ^ SID) ^ (PID_hi ^ PID_lo)) < 8
    // Let TID = 0x1234, SID = 0x5678.
    // TID ^ SID = 0x444C
    // Choose PID_hi = 0x444C, PID_lo = 0x0000 -> (PID_hi ^ PID_lo) = 0x444C
    // ((TID ^ SID) ^ (PID_hi ^ PID_lo)) = 0 < 8 -> SHINY!
    uint32_t otid_shiny = (0x5678U << 16) | 0x1234U;
    uint32_t pid_shiny  = (0x444CU << 16) | 0x0002U; // 0x444C ^ 0x0002 = 0x444E -> 0x444C ^ 0x444E = 2 < 8

    RawGbaPokemon raw_s;
    memset(&raw_s, 0, sizeof(raw_s));
    raw_s.pid = pid_shiny;
    raw_s.otid = otid_shiny;

    SubstructGrowth g = {.species = 6};
    SubstructAttacks a = {0};
    SubstructEVs e = {0};
    SubstructMisc m = {0};

    pack_and_encrypt(raw_s.pid, raw_s.otid,
                     (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                     raw_s.raw_substructures, &raw_s.checksum);

    ParsedPokemon shiny_mon;
    pokemon_parse_single((const uint8_t*)&raw_s, false, &shiny_mon);
    TEST_ASSERT(shiny_mon.is_shiny == true, "Calculated Pokemon should be shiny");

    // Non-shiny:
    uint32_t pid_non_shiny = (0x1111U << 16) | 0x2222U;
    raw_s.pid = pid_non_shiny;
    pack_and_encrypt(raw_s.pid, raw_s.otid,
                     (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                     raw_s.raw_substructures, &raw_s.checksum);

    ParsedPokemon regular_mon;
    pokemon_parse_single((const uint8_t*)&raw_s, false, &regular_mon);
    TEST_ASSERT(regular_mon.is_shiny == false, "Calculated Pokemon should NOT be shiny");

    g_tests_passed++;
    printf(ANSI_GREEN "  [PASS] test_shininess_calculation" ANSI_RESET "\n");
}

static void test_ewram_party_parsing(void) {
    printf("Running test_ewram_party_parsing (mock 256KB EWRAM)...\n");

    const size_t EWRAM_SIZE = 256 * 1024;
    uint8_t* ewram = (uint8_t*)calloc(1, EWRAM_SIZE);
    TEST_ASSERT(ewram != NULL, "Memory allocation for EWRAM failed");

    const GameMemoryConfig* emerald_cfg = pokemon_get_game_config(GAME_EMERALD);
    TEST_ASSERT(emerald_cfg != NULL, "Emerald config should exist");

    // Set player party count to 3
    ewram[emerald_cfg->player_party_count_offset] = 3;

    // Create 3 Pokemon in party
    for (int i = 0; i < 3; i++) {
        RawGbaPokemon raw;
        memset(&raw, 0, sizeof(raw));
        raw.pid = 0x1000 + (i * 24);
        raw.otid = 0x2000;
        raw.level = 20 + (i * 5);
        raw.max_hp = 50 + (i * 10);
        raw.current_hp = raw.max_hp;
        raw.attack = 30 + (i * 5);
        raw.defense = 30 + (i * 5);
        raw.speed = 30 + (i * 5);
        raw.sp_attack = 30 + (i * 5);
        raw.sp_defense = 30 + (i * 5);

        SubstructGrowth g = {.species = (uint16_t)(1 + i)}; // Bulbasaur, Ivysaur, Venusaur
        SubstructAttacks a = {.moves = {10, 20, 0, 0}, .pp = {30, 20, 0, 0}};
        SubstructEVs e = {.hp_ev = (uint8_t)(i * 10)};
        SubstructMisc m = {.iv_egg_ability = 31};

        pack_and_encrypt(raw.pid, raw.otid,
                         (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                         raw.raw_substructures, &raw.checksum);

        uint8_t* slot = ewram + emerald_cfg->player_party_offset + (i * sizeof(RawGbaPokemon));
        memcpy(slot, &raw, sizeof(RawGbaPokemon));
    }

    PartySnapshot snapshot;
    uint8_t parsed_count = pokemon_read_player_party(ewram, EWRAM_SIZE, emerald_cfg, &snapshot);

    TEST_ASSERT(parsed_count == 3, "Should have parsed 3 party Pokemon");
    TEST_ASSERT(snapshot.count == 3, "Snapshot count should be 3");
    TEST_ASSERT(snapshot.members[0].species == 1, "First member should be Bulbasaur (1)");
    TEST_ASSERT(snapshot.members[1].species == 2, "Second member should be Ivysaur (2)");
    TEST_ASSERT(snapshot.members[2].species == 3, "Third member should be Venusaur (3)");
    TEST_ASSERT(snapshot.members[0].level == 20, "First member level should be 20");
    TEST_ASSERT(snapshot.members[2].level == 30, "Third member level should be 30");

    free(ewram);
    g_tests_passed++;
    printf(ANSI_GREEN "  [PASS] test_ewram_party_parsing" ANSI_RESET "\n");
}

static void test_ewram_scan_ignores_box_pokemon_and_finds_real_party(void) {
    printf("Running test_ewram_scan_ignores_box_pokemon_and_finds_real_party...\n");

    const size_t EWRAM_SIZE = 256 * 1024;
    uint8_t* ewram = (uint8_t*)calloc(1, EWRAM_SIZE);
    TEST_ASSERT(ewram != NULL, "Memory allocation for EWRAM failed");

    pokemon_reader_reset();

    // 1. Place an 80-byte Box Pokémon (Numel, species 322) earlier in EWRAM at 0x10000
    // Box Pokémon do NOT have runtime battle stats (offsets 0x50..0x63 are 0)
    {
        RawGbaPokemon box_numel;
        memset(&box_numel, 0, sizeof(box_numel));
        box_numel.pid = 0x55443322;
        box_numel.otid = 0x99887766;
        // Nickname "NUMEL"
        uint8_t numel_name[] = {0xC8, 0xCE, 0xC7, 0xBF, 0xC6, 0xFF};
        memcpy(box_numel.nickname, numel_name, sizeof(numel_name));

        SubstructGrowth g = {.species = 322}; // Numel
        SubstructAttacks a = {0};
        SubstructEVs e = {0};
        SubstructMisc m = {.iv_egg_ability = 31};

        pack_and_encrypt(box_numel.pid, box_numel.otid,
                         (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                         box_numel.raw_substructures, &box_numel.checksum);

        // Crucially, level and max_hp are 0 for Box Pokemon
        box_numel.level = 0;
        box_numel.max_hp = 0;
        box_numel.current_hp = 0;
        box_numel.attack = 0;
        box_numel.defense = 0;

        memcpy(ewram + 0x10000, &box_numel, 80);
    }

    // 2. Place the player's true 3-mon party at 0x28000 (dynamic offset, e.g. Heart & Soul)
    // Put gPlayerPartyCount = 3 at 0x27FFC (off - 4)
    ewram[0x27FFC] = 3;

    for (int i = 0; i < 3; i++) {
        RawGbaPokemon party_mon;
        memset(&party_mon, 0, sizeof(party_mon));
        party_mon.pid = 0x7700 + (i * 24);
        party_mon.otid = 0x1234;
        party_mon.level = 25 + (i * 5);
        party_mon.max_hp = 60 + (i * 12);
        party_mon.current_hp = party_mon.max_hp;
        party_mon.attack = 40 + (i * 5);
        party_mon.defense = 35 + (i * 5);
        party_mon.speed = 45 + (i * 5);
        party_mon.sp_attack = 40 + (i * 5);
        party_mon.sp_defense = 40 + (i * 5);

        SubstructGrowth g = {.species = (uint16_t)(152 + i)}; // Chikorita, Bayleef, Meganium
        SubstructAttacks a = {.moves = {33, 75, 0, 0}, .pp = {25, 15, 0, 0}}; // Tackle, Razor Leaf
        SubstructEVs e = {.hp_ev = 10};
        SubstructMisc m = {.iv_egg_ability = 31};

        pack_and_encrypt(party_mon.pid, party_mon.otid,
                         (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                         party_mon.raw_substructures, &party_mon.checksum);

        uint8_t* slot = ewram + 0x28000 + (i * sizeof(RawGbaPokemon));
        memcpy(slot, &party_mon, sizeof(RawGbaPokemon));
    }

    // 3. Scan EWRAM
    PartySnapshot snapshot;
    uint8_t count = pokemon_scan_ewram_for_party(ewram, EWRAM_SIZE, &snapshot);

    TEST_ASSERT(count == 3, "Scanner must find all 3 genuine party members");
    TEST_ASSERT(snapshot.count == 3, "Snapshot count must be 3");
    TEST_ASSERT(snapshot.members[0].species == 152, "First member must be Chikorita (152), NOT Box Numel!");
    TEST_ASSERT(snapshot.members[1].species == 153, "Second member must be Bayleef (153)");
    TEST_ASSERT(snapshot.members[2].species == 154, "Third member must be Meganium (154)");
    TEST_ASSERT(snapshot.members[0].level == 25, "First member level must be 25");

    // 4. Test pokemon_read_player_party with mismatched static config offset
    // It should reject the invalid static offset and fallback to scanning, returning the true party
    const GameMemoryConfig* emerald_cfg = pokemon_get_game_config(GAME_EMERALD);
    pokemon_reader_reset();
    PartySnapshot read_snapshot;
    uint8_t read_count = pokemon_read_player_party(ewram, EWRAM_SIZE, emerald_cfg, &read_snapshot);
    TEST_ASSERT(read_count == 3, "pokemon_read_player_party must fallback and find the 3 party members");
    TEST_ASSERT(read_snapshot.members[0].species == 152, "First member must be Chikorita (152)");

    free(ewram);
    g_tests_passed++;
    printf(ANSI_GREEN "  [PASS] test_ewram_scan_ignores_box_pokemon_and_finds_real_party" ANSI_RESET "\n");
}

static void test_heart_and_soul_party_and_battle_hp_sync(void) {
    printf("Running test_heart_and_soul_party_and_battle_hp_sync...\n");

    const size_t EWRAM_SIZE = 256 * 1024;
    uint8_t* ewram = (uint8_t*)calloc(1, EWRAM_SIZE);
    TEST_ASSERT(ewram != NULL, "Memory allocation for EWRAM failed");

    pokemon_reader_reset();

    // 1. Verify game detection
    GbaGameId detected = pokemon_detect_game("POKEMON HEART SOUL");
    TEST_ASSERT(detected == GAME_HEART_AND_SOUL, "POKEMON HEART SOUL should detect as GAME_HEART_AND_SOUL");

    const GameMemoryConfig* hns_cfg = pokemon_get_game_config(GAME_HEART_AND_SOUL);
    TEST_ASSERT(hns_cfg != NULL, "Heart and Soul config must exist");
    TEST_ASSERT(hns_cfg->player_party_offset == 0x340F4, "HnS player party offset must be 0x340F4");
    TEST_ASSERT(hns_cfg->enemy_party_offset == 0x345A4, "HnS enemy party offset must be 0x345A4");
    TEST_ASSERT(hns_cfg->battle_mons_offset == 0x3A5A4, "HnS battle_mons offset must be 0x3A5A4");

    // 2. Setup Player Party (2 Pokemon: Cyndaquil #155 and Totodile #158)
    ewram[hns_cfg->player_party_count_offset] = 2;

    uint32_t player_otid = 0x88776655;

    // Cyndaquil in slot 0
    {
        RawGbaPokemon mon;
        memset(&mon, 0, sizeof(mon));
        mon.pid = 0x11223344;
        mon.otid = player_otid;
        mon.level = 14;
        mon.max_hp = 50;
        mon.current_hp = 50; // Full HP in static party
        mon.attack = 25;
        mon.defense = 22;
        mon.speed = 30;
        mon.sp_attack = 32;
        mon.sp_defense = 24;

        SubstructGrowth g = {.species = 155}; // Cyndaquil
        SubstructAttacks a = {.moves = {33, 52, 0, 0}, .pp = {35, 25, 0, 0}}; // Tackle, Ember
        SubstructEVs e = {0};
        SubstructMisc m = {.iv_egg_ability = 31};

        pack_and_encrypt(mon.pid, mon.otid,
                         (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                         mon.raw_substructures, &mon.checksum);
        memcpy(ewram + hns_cfg->player_party_offset, &mon, sizeof(RawGbaPokemon));
    }

    // Totodile in slot 1
    {
        RawGbaPokemon mon;
        memset(&mon, 0, sizeof(mon));
        mon.pid = 0x55667788;
        mon.otid = player_otid;
        mon.level = 15;
        mon.max_hp = 60;
        mon.current_hp = 60; // Full HP in static party
        mon.attack = 32;
        mon.defense = 30;
        mon.speed = 22;
        mon.sp_attack = 24;
        mon.sp_defense = 26;

        SubstructGrowth g = {.species = 158}; // Totodile
        SubstructAttacks a = {.moves = {33, 55, 0, 0}, .pp = {35, 30, 0, 0}}; // Tackle, Water Gun
        SubstructEVs e = {0};
        SubstructMisc m = {.iv_egg_ability = 31};

        pack_and_encrypt(mon.pid, mon.otid,
                         (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                         mon.raw_substructures, &mon.checksum);
        memcpy(ewram + hns_cfg->player_party_offset + sizeof(RawGbaPokemon), &mon, sizeof(RawGbaPokemon));
    }

    // 3. Setup Enemy Party (Pidgey #16 in enemy slot 0)
    ewram[hns_cfg->enemy_party_count_offset] = 1;
    {
        RawGbaPokemon enemy;
        memset(&enemy, 0, sizeof(enemy));
        enemy.pid = 0xAABBCCDD;
        enemy.otid = 0x99990000; // Different OTID
        enemy.level = 13;
        enemy.max_hp = 40;
        enemy.current_hp = 40;
        enemy.attack = 20;
        enemy.defense = 18;
        enemy.speed = 28;
        enemy.sp_attack = 18;
        enemy.sp_defense = 18;

        SubstructGrowth g = {.species = 16}; // Pidgey
        SubstructAttacks a = {.moves = {33, 16, 0, 0}, .pp = {35, 35, 0, 0}}; // Tackle, Gust
        SubstructEVs e = {0};
        SubstructMisc m = {.iv_egg_ability = 25};

        pack_and_encrypt(enemy.pid, enemy.otid,
                         (uint8_t*)&g, (uint8_t*)&a, (uint8_t*)&e, (uint8_t*)&m,
                         enemy.raw_substructures, &enemy.checksum);
        memcpy(ewram + hns_cfg->enemy_party_offset, &enemy, sizeof(RawGbaPokemon));
    }

    // 4. Populate live gBattleMons (BattlePokemon struct size = 96, HP at offset 46)
    // Battler 0: Player active mon (Cyndaquil #155), took damage! HP is 28/50
    uint8_t* b0 = ewram + hns_cfg->battle_mons_offset;
    b0[0] = 155 & 0xFF; // species low
    b0[1] = (155 >> 8) & 0xFF; // species high
    b0[46] = 28; // current HP low
    b0[47] = 0;  // current HP high

    // Battler 1: Enemy active mon (Pidgey #16), took damage! HP is 12/40
    uint8_t* b1 = ewram + hns_cfg->battle_mons_offset + hns_cfg->battle_mons_size;
    b1[0] = 16 & 0xFF; // species low
    b1[1] = (16 >> 8) & 0xFF; // species high
    b1[46] = 12; // current HP low
    b1[47] = 0;  // current HP high

    // 5. Read player party and verify live HP sync & active battler slot
    PartySnapshot player_snap;
    uint8_t player_count = pokemon_read_player_party(ewram, EWRAM_SIZE, hns_cfg, &player_snap);
    TEST_ASSERT(player_count == 2, "Player party count should be 2");
    TEST_ASSERT(player_snap.members[0].species == 155, "Slot 0 should be Cyndaquil");
    TEST_ASSERT(player_snap.members[0].current_hp == 28, "Cyndaquil HP should be 28 (live from gBattleMons!)");
    TEST_ASSERT(player_snap.members[1].species == 158, "Slot 1 should be Totodile");
    TEST_ASSERT(player_snap.members[1].current_hp == 60, "Totodile HP should remain 60");
    TEST_ASSERT(player_snap.active_battler_slot == 0, "Active battler slot should be 0 (Cyndaquil)");

    // 6. Read enemy party and verify live HP sync
    PartySnapshot enemy_snap;
    uint8_t enemy_count = pokemon_read_enemy_party(ewram, EWRAM_SIZE, hns_cfg, &enemy_snap);
    TEST_ASSERT(enemy_count == 1, "Enemy party count should be 1");
    TEST_ASSERT(enemy_snap.members[0].species == 16, "Enemy slot 0 should be Pidgey");
    TEST_ASSERT(enemy_snap.members[0].current_hp == 12, "Enemy Pidgey HP should be 12 (live from gBattleMons!)");

    // 7. Test mid-battle switch: player sends out Totodile (species 158), HP = 48
    b0[0] = 158 & 0xFF;
    b0[1] = (158 >> 8) & 0xFF;
    b0[46] = 48;
    b0[47] = 0;

    PartySnapshot switch_snap;
    pokemon_read_player_party(ewram, EWRAM_SIZE, hns_cfg, &switch_snap);
    TEST_ASSERT(switch_snap.active_battler_slot == 1, "Active battler slot should now be 1 (Totodile) after switch!");
    TEST_ASSERT(switch_snap.members[1].current_hp == 48, "Totodile HP should now be 48 (live from gBattleMons!)");

    free(ewram);
    g_tests_passed++;
    printf(ANSI_GREEN "  [PASS] test_heart_and_soul_party_and_battle_hp_sync" ANSI_RESET "\n");
}

int main(void) {
    printf("===================================================\n");
    printf("   DualDex Gen 3 Memory Parser Test Suite\n");
    printf("===================================================\n");

    test_text_decoding();
    test_single_pokemon_decryption();
    test_all_substructure_orders();
    test_checksum_corruption_detection();
    test_shininess_calculation();
    test_ewram_party_parsing();
    test_ewram_scan_ignores_box_pokemon_and_finds_real_party();
    test_heart_and_soul_party_and_battle_hp_sync();

    printf("===================================================\n");
    printf("Results: %d Passed, %d Failed\n", g_tests_passed, g_tests_failed);
    printf("===================================================\n");

    return (g_tests_failed == 0) ? 0 : 1;
}
