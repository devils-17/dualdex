package com.dualdex.pokemon

enum class MoveCategory(val displayName: String) {
    PHYSICAL("Physical"),
    SPECIAL("Special"),
    STATUS("Status")
}

data class MoveInfo(
    val id: Int,
    val name: String,
    val type: PokemonType,
    val category: MoveCategory,
    val power: Int,
    val accuracy: Int,
    val pp: Int
)

object MoveDatabase {
    private val moveMap = mutableMapOf<Int, MoveInfo>()
    private val nameMap = mutableMapOf<String, MoveInfo>()

    private fun register(id: Int, name: String, type: PokemonType, category: MoveCategory, power: Int, acc: Int, pp: Int) {
        val info = MoveInfo(id, name, type, category, power, acc, pp)
        moveMap[id] = info
        nameMap[name.lowercase()] = info
    }

    init {
        // Gen 1-3 staple moves
        register(1, "Pound", PokemonType.NORMAL, MoveCategory.PHYSICAL, 40, 100, 35)
        register(2, "Karate Chop", PokemonType.FIGHTING, MoveCategory.PHYSICAL, 50, 100, 25)
        register(10, "Scratch", PokemonType.NORMAL, MoveCategory.PHYSICAL, 40, 100, 35)
        register(33, "Tackle", PokemonType.NORMAL, MoveCategory.PHYSICAL, 40, 100, 35)
        register(52, "Ember", PokemonType.FIRE, MoveCategory.SPECIAL, 40, 100, 25)
        register(53, "Flamethrower", PokemonType.FIRE, MoveCategory.SPECIAL, 90, 100, 15)
        register(55, "Water Gun", PokemonType.WATER, MoveCategory.SPECIAL, 40, 100, 25)
        register(56, "Hydro Pump", PokemonType.WATER, MoveCategory.SPECIAL, 110, 80, 5)
        register(57, "Surf", PokemonType.WATER, MoveCategory.SPECIAL, 90, 100, 15)
        register(58, "Ice Beam", PokemonType.ICE, MoveCategory.SPECIAL, 90, 100, 10)
        register(59, "Blizzard", PokemonType.ICE, MoveCategory.SPECIAL, 110, 70, 5)
        register(75, "Razor Leaf", PokemonType.GRASS, MoveCategory.PHYSICAL, 55, 95, 25)
        register(76, "Solar Beam", PokemonType.GRASS, MoveCategory.SPECIAL, 120, 100, 10)
        register(85, "Thunderbolt", PokemonType.ELECTRIC, MoveCategory.SPECIAL, 90, 100, 15)
        register(87, "Thunder", PokemonType.ELECTRIC, MoveCategory.SPECIAL, 110, 70, 10)
        register(89, "Earthquake", PokemonType.GROUND, MoveCategory.PHYSICAL, 100, 100, 10)
        register(91, "Dig", PokemonType.GROUND, MoveCategory.PHYSICAL, 80, 100, 10)
        register(92, "Toxic", PokemonType.POISON, MoveCategory.STATUS, 0, 90, 10)
        register(94, "Psychic", PokemonType.PSYCHIC, MoveCategory.SPECIAL, 90, 100, 10)
        register(97, "Agility", PokemonType.PSYCHIC, MoveCategory.STATUS, 0, 0, 30)
        register(100, "Teleport", PokemonType.PSYCHIC, MoveCategory.STATUS, 0, 0, 20)
        register(104, "Double Team", PokemonType.NORMAL, MoveCategory.STATUS, 0, 0, 15)
        register(105, "Recover", PokemonType.NORMAL, MoveCategory.STATUS, 0, 0, 10)
        register(113, "Light Screen", PokemonType.PSYCHIC, MoveCategory.STATUS, 0, 0, 30)
        register(115, "Reflect", PokemonType.PSYCHIC, MoveCategory.STATUS, 0, 0, 20)
        register(147, "Spore", PokemonType.GRASS, MoveCategory.STATUS, 0, 100, 15)
        register(157, "Rock Slide", PokemonType.ROCK, MoveCategory.PHYSICAL, 75, 90, 10)
        register(164, "Substitute", PokemonType.NORMAL, MoveCategory.STATUS, 0, 0, 10)
        register(174, "Curse", PokemonType.GHOST, MoveCategory.STATUS, 0, 0, 10)
        register(182, "Protect", PokemonType.NORMAL, MoveCategory.STATUS, 0, 0, 10)
        register(202, "Giga Drain", PokemonType.GRASS, MoveCategory.SPECIAL, 75, 100, 10)
        register(210, "Fury Cutter", PokemonType.BUG, MoveCategory.PHYSICAL, 40, 95, 20)
        register(214, "Sleep Talk", PokemonType.NORMAL, MoveCategory.STATUS, 0, 0, 10)
        register(226, "Baton Pass", PokemonType.NORMAL, MoveCategory.STATUS, 0, 0, 40)
        register(237, "Hidden Power", PokemonType.NORMAL, MoveCategory.SPECIAL, 60, 100, 15)
        register(240, "Rain Dance", PokemonType.WATER, MoveCategory.STATUS, 0, 0, 5)
        register(241, "Sunny Day", PokemonType.FIRE, MoveCategory.STATUS, 0, 0, 5)
        register(242, "Crunch", PokemonType.DARK, MoveCategory.PHYSICAL, 80, 100, 15)
        register(247, "Shadow Ball", PokemonType.GHOST, MoveCategory.SPECIAL, 80, 100, 15)
        register(249, "Rock Smash", PokemonType.FIGHTING, MoveCategory.PHYSICAL, 40, 100, 15)
        register(280, "Brick Break", PokemonType.FIGHTING, MoveCategory.PHYSICAL, 75, 100, 15)
        register(337, "Dragon Claw", PokemonType.DRAGON, MoveCategory.PHYSICAL, 80, 100, 15)
        register(339, "Bulk Up", PokemonType.FIGHTING, MoveCategory.STATUS, 0, 0, 20)
        register(340, "Bounce", PokemonType.FLYING, MoveCategory.PHYSICAL, 85, 85, 5)
        register(347, "Calm Mind", PokemonType.PSYCHIC, MoveCategory.STATUS, 0, 0, 20)
        register(354, "Psycho Boost", PokemonType.PSYCHIC, MoveCategory.SPECIAL, 140, 90, 5)

        // Expanded Gen 4+ Moves (used in Radical Red & modern hacks)
        register(370, "Close Combat", PokemonType.FIGHTING, MoveCategory.PHYSICAL, 120, 100, 5)
        register(394, "Flare Blitz", PokemonType.FIRE, MoveCategory.PHYSICAL, 120, 100, 15)
        register(402, "Seed Bomb", PokemonType.GRASS, MoveCategory.PHYSICAL, 80, 100, 15)
        register(405, "Bug Buzz", PokemonType.BUG, MoveCategory.SPECIAL, 90, 100, 10)
        register(411, "Focus Blast", PokemonType.FIGHTING, MoveCategory.SPECIAL, 120, 70, 5)
        register(416, "Giga Impact", PokemonType.NORMAL, MoveCategory.PHYSICAL, 150, 90, 5)
        register(585, "Moonblast", PokemonType.FAIRY, MoveCategory.SPECIAL, 95, 100, 15)
        register(586, "Play Rough", PokemonType.FAIRY, MoveCategory.PHYSICAL, 90, 90, 10)
    }

    fun get(moveId: Int): MoveInfo {
        if (moveId <= 0) {
            return MoveInfo(0, "-", PokemonType.NORMAL, MoveCategory.STATUS, 0, 0, 0)
        }
        return moveMap[moveId] ?: MoveInfo(
            id = moveId,
            name = "Move #$moveId",
            type = PokemonType.NORMAL,
            category = MoveCategory.PHYSICAL,
            power = 50,
            accuracy = 100,
            pp = 20
        )
    }

    fun getByName(name: String): MoveInfo? {
        return nameMap[name.lowercase()]
    }
}
