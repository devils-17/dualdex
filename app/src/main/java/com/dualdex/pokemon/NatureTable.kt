package com.dualdex.pokemon

enum class StatType {
    ATTACK, DEFENSE, SPEED, SP_ATTACK, SP_DEFENSE, NONE
}

data class NatureInfo(
    val id: Int,
    val name: String,
    val increasedStat: StatType,
    val decreasedStat: StatType
) {
    val isNeutral: Boolean get() = increasedStat == StatType.NONE || increasedStat == decreasedStat

    val formattedDescription: String
        get() {
            if (isNeutral) return "$name (Neutral)"
            val inc = formatStat(increasedStat)
            val dec = formatStat(decreasedStat)
            return "$name (+$inc, -$dec)"
        }

    private fun formatStat(stat: StatType): String = when (stat) {
        StatType.ATTACK -> "Atk"
        StatType.DEFENSE -> "Def"
        StatType.SPEED -> "Spe"
        StatType.SP_ATTACK -> "SpA"
        StatType.SP_DEFENSE -> "SpD"
        StatType.NONE -> "None"
    }

    fun getModifier(stat: StatType): Float {
        if (isNeutral) return 1.0f
        return when {
            stat == increasedStat -> 1.1f
            stat == decreasedStat -> 0.9f
            else -> 1.0f
        }
    }
}

object NatureTable {
    private val natures = listOf(
        NatureInfo(0, "Hardy", StatType.NONE, StatType.NONE),
        NatureInfo(1, "Lonely", StatType.ATTACK, StatType.DEFENSE),
        NatureInfo(2, "Brave", StatType.ATTACK, StatType.SPEED),
        NatureInfo(3, "Adamant", StatType.ATTACK, StatType.SP_ATTACK),
        NatureInfo(4, "Naughty", StatType.ATTACK, StatType.SP_DEFENSE),
        NatureInfo(5, "Bold", StatType.DEFENSE, StatType.ATTACK),
        NatureInfo(6, "Docile", StatType.NONE, StatType.NONE),
        NatureInfo(7, "Relaxed", StatType.DEFENSE, StatType.SPEED),
        NatureInfo(8, "Impish", StatType.DEFENSE, StatType.SP_ATTACK),
        NatureInfo(9, "Lax", StatType.DEFENSE, StatType.SP_DEFENSE),
        NatureInfo(10, "Timid", StatType.SPEED, StatType.ATTACK),
        NatureInfo(11, "Hasty", StatType.SPEED, StatType.DEFENSE),
        NatureInfo(12, "Serious", StatType.NONE, StatType.NONE),
        NatureInfo(13, "Jolly", StatType.SPEED, StatType.SP_ATTACK),
        NatureInfo(14, "Naive", StatType.SPEED, StatType.SP_DEFENSE),
        NatureInfo(15, "Modest", StatType.SP_ATTACK, StatType.ATTACK),
        NatureInfo(16, "Mild", StatType.SP_ATTACK, StatType.DEFENSE),
        NatureInfo(17, "Quiet", StatType.SP_ATTACK, StatType.SPEED),
        NatureInfo(18, "Bashful", StatType.NONE, StatType.NONE),
        NatureInfo(19, "Rash", StatType.SP_ATTACK, StatType.SP_DEFENSE),
        NatureInfo(20, "Calm", StatType.SP_DEFENSE, StatType.ATTACK),
        NatureInfo(21, "Gentle", StatType.SP_DEFENSE, StatType.DEFENSE),
        NatureInfo(22, "Sassy", StatType.SP_DEFENSE, StatType.SPEED),
        NatureInfo(23, "Careful", StatType.SP_DEFENSE, StatType.SP_ATTACK),
        NatureInfo(24, "Quirky", StatType.NONE, StatType.NONE)
    )

    fun get(natureId: Int): NatureInfo {
        val safeId = if (natureId in 0..24) natureId else (natureId % 25)
        return natures[safeId]
    }

    fun getByName(name: String): NatureInfo {
        return natures.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: natures[0]
    }

    fun getAll(): List<NatureInfo> = natures
}
