package com.dualdex.calculator

data class CalcPokemonInput(
    val species: String,
    val level: Int = 50,
    val item: String? = null,
    val nature: String? = null,
    val ability: String? = null,
    val curHP: Int? = null,
    val ivs: StatBlock? = null,
    val evs: StatBlock? = null,
    val boosts: StatBlock? = null,
    val status: String? = null
)

data class StatBlock(
    val hp: Int = 0,
    val atk: Int = 0,
    val def: Int = 0,
    val spa: Int = 0,
    val spd: Int = 0,
    val spe: Int = 0
)

data class CalcMoveInput(
    val name: String,
    val isCrit: Boolean = false
)

data class CalcFieldInput(
    val gameType: String = "singles",
    val weather: String? = null,
    val terrain: String? = null
)

data class DamageCalculationRequest(
    val gen: Int = 3,
    val attacker: CalcPokemonInput,
    val defender: CalcPokemonInput,
    val move: CalcMoveInput,
    val field: CalcFieldInput = CalcFieldInput()
)

data class DamageCalculationResponse(
    val success: Boolean,
    val minDamage: Int = 0,
    val maxDamage: Int = 0,
    val range: List<Int> = emptyList(),
    val desc: String = "",
    val moveName: String = "",
    val moveCategory: String = "",
    val moveType: String = "",
    val movePower: Int = 0,
    val attackerName: String = "",
    val defenderName: String = "",
    val defenderMaxHP: Int = 0,
    val koChanceText: String = "",
    val error: String? = null
)
