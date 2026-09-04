#include "pokemon_reader.h"
#include "pokemon_text.h"
#include <string.h>
#include <stdio.h>

static const char* NATURE_NAMES[25] = {
    "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
    "Bold", "Docile", "Relaxed", "Impish", "Lax",
    "Timid", "Hasty", "Serious", "Jolly", "Naive",
    "Modest", "Mild", "Quiet", "Bashful", "Rash",
    "Calm", "Gentle", "Sassy", "Careful", "Quirky"
};

// Substructure block indices for [G, A, E, M] in the 48-byte decrypted array (multiples of 12 bytes)
// Derived from the standard 24 permutation orders of GAEM
static const uint8_t SUBSTRUCT_BLOCK_INDEX[24][4] = {
    // G, A, E, M
    {0, 1, 2, 3}, // 0:  GAEM
    {0, 1, 3, 2}, // 1:  GAME
    {0, 2, 1, 3}, // 2:  GEAM
    {0, 3, 1, 2}, // 3:  GEMA
    {0, 2, 3, 1}, // 4:  GMAE
    {0, 3, 2, 1}, // 5:  GMEA
    {1, 0, 2, 3}, // 6:  AGEM
    {1, 0, 3, 2}, // 7:  AGME
    {2, 0, 1, 3}, // 8:  AEGM
    {3, 0, 1, 2}, // 9:  AEMG
    {2, 0, 3, 1}, // 10: AMGE
    {3, 0, 2, 1}, // 11: AMEG
    {1, 2, 0, 3}, // 12: EGAM
    {1, 3, 0, 2}, // 13: EGMA
    {2, 1, 0, 3}, // 14: EAGM
    {3, 1, 0, 2}, // 15: EAMG
    {2, 3, 0, 1}, // 16: EMGA
    {3, 2, 0, 1}, // 17: EMAG
    {1, 2, 3, 0}, // 18: MGAE
    {1, 3, 2, 0}, // 19: MGEA
    {2, 1, 3, 0}, // 20: MAGE
    {3, 1, 2, 0}, // 21: MAEG
    {2, 3, 1, 0}, // 22: MEGA
    {3, 2, 1, 0}  // 23: MEAG
};

// Standard game configurations
static const GameMemoryConfig CONFIG_EMERALD = {
    .game_id = GAME_EMERALD,
    .game_name = "Pokemon Emerald",
    .player_party_offset = 0x244EC,
    .player_party_count_offset = 0x244E9,
    .enemy_party_offset = 0x24744,
    .enemy_party_count_offset = 0x24740,
    .has_evs = true,
    .has_ivs = true
};

static const GameMemoryConfig CONFIG_FIRERED = {
    .game_id = GAME_FIRERED,
    .game_name = "Pokemon FireRed",
    .player_party_offset = 0x24284,
    .player_party_count_offset = 0x24029,
    .enemy_party_offset = 0x2402C,
    .enemy_party_count_offset = 0x24028,
    .has_evs = true,
    .has_ivs = true
};

static const GameMemoryConfig CONFIG_LEAFGREEN = {
    .game_id = GAME_LEAFGREEN,
    .game_name = "Pokemon LeafGreen",
    .player_party_offset = 0x24284,
    .player_party_count_offset = 0x24029,
    .enemy_party_offset = 0x2402C,
    .enemy_party_count_offset = 0x24028,
    .has_evs = true,
    .has_ivs = true
};

static const GameMemoryConfig CONFIG_RUBY = {
    .game_id = GAME_RUBY,
    .game_name = "Pokemon Ruby",
    .player_party_offset = 0x24490,
    .player_party_count_offset = 0x2448C,
    .enemy_party_offset = 0x246E8,
    .enemy_party_count_offset = 0x246E4,
    .has_evs = true,
    .has_ivs = true
};

static const GameMemoryConfig CONFIG_SAPPHIRE = {
    .game_id = GAME_SAPPHIRE,
    .game_name = "Pokemon Sapphire",
    .player_party_offset = 0x24490,
    .player_party_count_offset = 0x2448C,
    .enemy_party_offset = 0x246E8,
    .enemy_party_count_offset = 0x246E4,
    .has_evs = true,
    .has_ivs = true
};

static const GameMemoryConfig CONFIG_GHOST_GREY = {
    .game_id = GAME_GHOST_GREY,
    .game_name = "Pokemon Ghost Grey",
    .player_party_offset = 0x24284,
    .player_party_count_offset = 0x24029,
    .enemy_party_offset = 0x2402C,
    .enemy_party_count_offset = 0x24028,
    .has_evs = false, // Ghost Grey removes EVs
    .has_ivs = false  // Ghost Grey removes IVs
};

static const GameMemoryConfig CONFIG_RADICAL_RED = {
    .game_id = GAME_RADICAL_RED,
    .game_name = "Pokemon Radical Red",
    .player_party_offset = 0x24284,
    .player_party_count_offset = 0x24029,
    .enemy_party_offset = 0x2402C,
    .enemy_party_count_offset = 0x24028,
    .has_evs = true,
    .has_ivs = true
};

const char* pokemon_get_nature_name(uint8_t nature_index) {
    if (nature_index < 25) {
        return NATURE_NAMES[nature_index];
    }
    return "Unknown";
}

GbaGameId pokemon_detect_game(const char* rom_title_16) {
    if (!rom_title_16) return GAME_UNKNOWN;

    char title_buf[17] = {0};
    strncpy(title_buf, rom_title_16, 16);
    title_buf[16] = '\0';

    // Check title in ROM header (offset 0xA0)
    if (strncmp(title_buf, "POKEMON EMER", 12) == 0) return GAME_EMERALD;
    if (strncmp(title_buf, "POKEMON FIRE", 12) == 0) return GAME_FIRERED;
    if (strncmp(title_buf, "POKEMON LEAF", 12) == 0) return GAME_LEAFGREEN;
    if (strncmp(title_buf, "POKEMON RUBY", 12) == 0) return GAME_RUBY;
    if (strncmp(title_buf, "POKEMON SAPP", 12) == 0) return GAME_SAPPHIRE;

    // Check custom hack headers if present
    if (strstr(title_buf, "GHOST") != NULL || strstr(title_buf, "GREY") != NULL) {
        return GAME_GHOST_GREY;
    }
    if (strstr(title_buf, "RADICAL") != NULL) {
        return GAME_RADICAL_RED;
    }
    if (strstr(title_buf, "HEARTSOUL") != NULL || strstr(title_buf, "HNS") != NULL || strstr(title_buf, "HEART") != NULL) {
        return GAME_EMERALD;
    }

    return GAME_UNKNOWN;
}

const GameMemoryConfig* pokemon_get_game_config(GbaGameId game_id) {
    switch (game_id) {
        case GAME_EMERALD: return &CONFIG_EMERALD;
        case GAME_FIRERED: return &CONFIG_FIRERED;
        case GAME_LEAFGREEN: return &CONFIG_LEAFGREEN;
        case GAME_RUBY: return &CONFIG_RUBY;
        case GAME_SAPPHIRE: return &CONFIG_SAPPHIRE;
        case GAME_GHOST_GREY: return &CONFIG_GHOST_GREY;
        case GAME_RADICAL_RED: return &CONFIG_RADICAL_RED;
        default: return &CONFIG_FIRERED; // Default fallback
    }
}

static inline uint16_t read16_le(const uint8_t* ptr) {
    return (uint16_t)ptr[0] | ((uint16_t)ptr[1] << 8);
}

static inline uint32_t read32_le(const uint8_t* ptr) {
    return (uint32_t)ptr[0] |
           ((uint32_t)ptr[1] << 8) |
           ((uint32_t)ptr[2] << 16) |
           ((uint32_t)ptr[3] << 24);
}

bool pokemon_parse_single(const uint8_t* raw_bytes, bool is_party_mon, ParsedPokemon* out) {
    if (!raw_bytes || !out) return false;

    memset(out, 0, sizeof(ParsedPokemon));

    const RawGbaPokemon* raw = (const RawGbaPokemon*)raw_bytes;

    // Check if empty slot
    if (raw->pid == 0 && raw->otid == 0) {
        out->is_empty = true;
        return false;
    }

    out->pid = raw->pid;
    out->tid = (uint16_t)(raw->otid & 0xFFFF);
    out->sid = (uint16_t)((raw->otid >> 16) & 0xFFFF);

    // Decode nickname & OT name
    pokemon_decode_string(raw->nickname, sizeof(raw->nickname), out->nickname, sizeof(out->nickname));
    pokemon_decode_string(raw->ot_name, sizeof(raw->ot_name), out->ot_name, sizeof(out->ot_name));

    // 1. Decrypt 48 bytes of substructures
    uint32_t key = raw->pid ^ raw->otid;
    uint32_t decrypted_words[12];
    const uint8_t* raw_subs = raw->raw_substructures;

    for (int i = 0; i < 12; i++) {
        uint32_t word = read32_le(raw_subs + (i * 4));
        decrypted_words[i] = word ^ key;
    }

    // 2. Validate Checksum over 24 16-bit half-words
    uint16_t calc_checksum = 0;
    const uint8_t* dec_u8 = (const uint8_t*)decrypted_words;
    for (int i = 0; i < 24; i++) {
        calc_checksum += read16_le(dec_u8 + (i * 2));
    }

    if (calc_checksum != raw->checksum) {
        out->is_valid = false;
        return false;
    }
    out->is_valid = true;

    // 3. Locate substructures G, A, E, M using permutation table
    uint32_t order = raw->pid % 24;
    const uint8_t* dec_bytes = (const uint8_t*)decrypted_words;

    const uint8_t* g_ptr = dec_bytes + (SUBSTRUCT_BLOCK_INDEX[order][0] * 12);
    const uint8_t* a_ptr = dec_bytes + (SUBSTRUCT_BLOCK_INDEX[order][1] * 12);
    const uint8_t* e_ptr = dec_bytes + (SUBSTRUCT_BLOCK_INDEX[order][2] * 12);
    const uint8_t* m_ptr = dec_bytes + (SUBSTRUCT_BLOCK_INDEX[order][3] * 12);

    // 4. Growth (G)
    out->species = read16_le(g_ptr + 0);
    out->held_item = read16_le(g_ptr + 2);
    out->experience = read32_le(g_ptr + 4);
    uint8_t pp_bonuses = g_ptr[8];
    out->pp_bonuses[0] = (pp_bonuses >> 0) & 0x03;
    out->pp_bonuses[1] = (pp_bonuses >> 2) & 0x03;
    out->pp_bonuses[2] = (pp_bonuses >> 4) & 0x03;
    out->pp_bonuses[3] = (pp_bonuses >> 6) & 0x03;
    out->friendship = g_ptr[9];

    // 5. Attacks (A)
    for (int i = 0; i < 4; i++) {
        out->moves[i] = read16_le(a_ptr + (i * 2));
        out->pp[i] = a_ptr[8 + i];
    }

    // 6. EVs (E)
    out->hp_ev = e_ptr[0];
    out->attack_ev = e_ptr[1];
    out->defense_ev = e_ptr[2];
    out->speed_ev = e_ptr[3];
    out->sp_attack_ev = e_ptr[4];
    out->sp_defense_ev = e_ptr[5];

    // 7. Misc (M): IVs, Egg, Ability
    uint32_t iv_word = read32_le(m_ptr + 4);
    out->hp_iv = (uint8_t)((iv_word >> 0) & 0x1F);
    out->attack_iv = (uint8_t)((iv_word >> 5) & 0x1F);
    out->defense_iv = (uint8_t)((iv_word >> 10) & 0x1F);
    out->speed_iv = (uint8_t)((iv_word >> 15) & 0x1F);
    out->sp_attack_iv = (uint8_t)((iv_word >> 20) & 0x1F);
    out->sp_defense_iv = (uint8_t)((iv_word >> 25) & 0x1F);
    out->is_egg = (bool)((iv_word >> 30) & 0x01);
    out->ability_slot = (uint8_t)((iv_word >> 31) & 0x01);

    // 8. Derived Properties
    out->nature = (uint8_t)(out->pid % 25);
    out->nature_name = pokemon_get_nature_name(out->nature);

    uint16_t pid_hi = (uint16_t)((out->pid >> 16) & 0xFFFF);
    uint16_t pid_lo = (uint16_t)(out->pid & 0xFFFF);
    out->is_shiny = (((out->tid ^ out->sid) ^ (pid_hi ^ pid_lo)) < 8);

    // 9. Battle stats (if party Pokémon, offsets 0x50 - 0x63)
    if (is_party_mon) {
        out->status_condition = raw->status_condition;
        out->level = raw->level;
        out->current_hp = raw->current_hp;
        out->max_hp = raw->max_hp;
        out->attack = raw->attack;
        out->defense = raw->defense;
        out->speed = raw->speed;
        out->sp_attack = raw->sp_attack;
        out->sp_defense = raw->sp_defense;
    } else {
        out->level = 0;
    }

    return true;
}

uint8_t pokemon_read_player_party(
    const uint8_t* ewram,
    size_t ewram_size,
    const GameMemoryConfig* config,
    PartySnapshot* out_snapshot
) {
    if (!ewram || !config || !out_snapshot) return 0;

    memset(out_snapshot, 0, sizeof(PartySnapshot));

    // Verify offsets fit within EWRAM buffer
    if (config->player_party_count_offset >= ewram_size ||
        config->player_party_offset + (6 * sizeof(RawGbaPokemon)) > ewram_size) {
        return 0;
    }

    uint8_t party_count = ewram[config->player_party_count_offset];
    if (party_count > 6) party_count = 6;

    uint8_t valid_count = 0;
    for (uint8_t i = 0; i < party_count; i++) {
        const uint8_t* mon_ptr = ewram + config->player_party_offset + (i * sizeof(RawGbaPokemon));
        if (pokemon_parse_single(mon_ptr, true, &out_snapshot->members[valid_count])) {
            if (!config->has_evs) {
                out_snapshot->members[valid_count].hp_ev = 0;
                out_snapshot->members[valid_count].attack_ev = 0;
                out_snapshot->members[valid_count].defense_ev = 0;
                out_snapshot->members[valid_count].speed_ev = 0;
                out_snapshot->members[valid_count].sp_attack_ev = 0;
                out_snapshot->members[valid_count].sp_defense_ev = 0;
            }
            if (!config->has_ivs) {
                out_snapshot->members[valid_count].hp_iv = 0;
                out_snapshot->members[valid_count].attack_iv = 0;
                out_snapshot->members[valid_count].defense_iv = 0;
                out_snapshot->members[valid_count].speed_iv = 0;
                out_snapshot->members[valid_count].sp_attack_iv = 0;
                out_snapshot->members[valid_count].sp_defense_iv = 0;
            }
            valid_count++;
        }
    }

    out_snapshot->count = valid_count;
    return valid_count;
}

uint8_t pokemon_read_enemy_party(
    const uint8_t* ewram,
    size_t ewram_size,
    const GameMemoryConfig* config,
    PartySnapshot* out_snapshot
) {
    if (!ewram || !config || !out_snapshot) return 0;

    memset(out_snapshot, 0, sizeof(PartySnapshot));

    if (config->enemy_party_count_offset >= ewram_size ||
        config->enemy_party_offset + (6 * sizeof(RawGbaPokemon)) > ewram_size) {
        return 0;
    }

    uint8_t enemy_count = ewram[config->enemy_party_count_offset];
    if (enemy_count > 6) enemy_count = 6;

    uint8_t valid_count = 0;
    for (uint8_t i = 0; i < enemy_count; i++) {
        const uint8_t* mon_ptr = ewram + config->enemy_party_offset + (i * sizeof(RawGbaPokemon));
        if (pokemon_parse_single(mon_ptr, true, &out_snapshot->members[valid_count])) {
            if (!config->has_evs) {
                out_snapshot->members[valid_count].hp_ev = 0;
                out_snapshot->members[valid_count].attack_ev = 0;
                out_snapshot->members[valid_count].defense_ev = 0;
                out_snapshot->members[valid_count].speed_ev = 0;
                out_snapshot->members[valid_count].sp_attack_ev = 0;
                out_snapshot->members[valid_count].sp_defense_ev = 0;
            }
            if (!config->has_ivs) {
                out_snapshot->members[valid_count].hp_iv = 0;
                out_snapshot->members[valid_count].attack_iv = 0;
                out_snapshot->members[valid_count].defense_iv = 0;
                out_snapshot->members[valid_count].speed_iv = 0;
                out_snapshot->members[valid_count].sp_attack_iv = 0;
                out_snapshot->members[valid_count].sp_defense_iv = 0;
            }
            valid_count++;
        }
    }

    out_snapshot->count = valid_count;
    return valid_count;
}
