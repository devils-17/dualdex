package com.dualdex.pokemon

/**
 * Kotlin data class representing a parsed Pokémon from memory or save state.
 */
data class ParsedPokemon(
    val isValid: Boolean,
    val isEmpty: Boolean,
    val pid: Long,
    val tid: Int,
    val sid: Int,
    val nickname: String,
    val otName: String,
    val species: Int,
    val heldItem: Int,
    val level: Int,
    val nature: Int,
    val natureName: String,
    val isShiny: Boolean,
    val abilitySlot: Int,
    val isEgg: Boolean,
    val friendship: Int,
    val experience: Long,
    // IVs (0 - 31)
    val hpIv: Int,
    val attackIv: Int,
    val defenseIv: Int,
    val speedIv: Int,
    val spAttackIv: Int,
    val spDefenseIv: Int,
    // EVs (0 - 255)
    val hpEv: Int,
    val attackEv: Int,
    val defenseEv: Int,
    val speedEv: Int,
    val spAttackEv: Int,
    val spDefenseEv: Int,
    // Moves & PP
    val moves: IntArray,
    val pp: IntArray,
    // Runtime battle stats
    val currentHp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val spAttack: Int,
    val spDefense: Int,
    val statusCondition: Long
) {
    val totalEvs: Int get() = hpEv + attackEv + defenseEv + speedEv + spAttackEv + spDefenseEv
    val totalIvs: Int get() = hpIv + attackIv + defenseIv + speedIv + spAttackIv + spDefenseIv
}

enum class GbaGame(val id: Int, val title: String) {
    UNKNOWN(0, "Unknown"),
    EMERALD(1, "Pokemon Emerald"),
    FIRERED(2, "Pokemon FireRed"),
    LEAFGREEN(3, "Pokemon LeafGreen"),
    RUBY(4, "Pokemon Ruby"),
    SAPPHIRE(5, "Pokemon Sapphire"),
    GHOST_GREY(6, "Pokemon Ghost Grey"),
    RADICAL_RED(7, "Pokemon Radical Red");

    companion object {
        fun fromId(id: Int): GbaGame = values().firstOrNull { it.id == id } ?: UNKNOWN
    }
}
